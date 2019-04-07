(ns timecap.events
  (:require
    [timecap.db    :refer [default-db timecap->local-store]]
    [re-frame.core :refer [reg-event-db reg-event-fx inject-cofx path after]]
    [cljs.spec.alpha :as s]))

;; Check that the state is correct
(defn check-and-throw
  "Throws an exception if `db` doesn't match the Spec `a-spec`."
  [a-spec db]
  (when-not (s/valid? a-spec db)
    (throw (ex-info (str "spec check failed: " (s/explain-str a-spec db)) {}))))
;; now we create an interceptor using `after`
(def check-spec-interceptor (after (partial check-and-throw :timecap.db/db)))

;; Store to local storage
(def ->local-store (after timecap->local-store))

;; -- Interceptor Chain ------------------------------------------------------
(def todo-interceptors [check-spec-interceptor 
                        (path :entries)
                        ->local-store])

;; -- Helpers -----------------------------------------------------------------

(defn allocate-next-id
  "Returns the next todo id.
  Assumes todos are sorted.
  Returns one more than the current largest id."
  [entries]
  ((fnil inc 0) (last (keys entries))))

;; -- Event Handlers ----------------------------------------------------------

;; usage:  (dispatch [:initialise-db])
;;
;; This event is dispatched in the app's `main` (core.cljs).
;; It establishes initial application state in `app-db`.
;; That means merging:
;;   1. Any todos stored in LocalStore (from the last session of this app)
;;   2. Default initial values
;;
;; Advanced topic:  we inject the todos currently stored in LocalStore
;; into the first, coeffect parameter via use of the interceptor
;;    `(inject-cofx :local-store-timecap)`
; TODO restore this better init
; (reg-event-fx                 ;; part of the re-frame API
;   :initialise-db              ;; event id being handled

;   ;; the interceptor chain (a vector of 2 interceptors in this case)
;   [(inject-cofx :local-store-timecap) ;; gets todos from localstore, and puts value into coeffects arg
;    check-spec-interceptor]          ;; after event handler runs, check app-db for correctness. Does it still match Spec?

;   ;; the event handler (function) being registered
;   (fn [{:keys [db local-content]} _]                  ;; take 2 values from coeffects. Ignore event vector itself.
;     {:db (assoc default-db :entries local-content)}))   ;; all hail the new state to be put in app-db
(reg-event-db
  :initialise-db
  (fn [_ _]
    default-db))

;; usage:  (dispatch [:set-showing  :active])
;; This event is dispatched when the user clicks on one of the 3
;; filter buttons at the bottom of the display.
; (reg-event-db      ;; part of the re-frame API
;   :set-showing     ;; event-id

;   ;; only one interceptor
;   [check-spec-interceptor]       ;; after event handler runs, check app-db for correctness. Does it still match Spec?

;   ;; handler
;   (fn [db [_ new-filter-kw]]     ;; new-filter-kw is one of :all, :active or :done
;     (assoc db :showing new-filter-kw)))

;; NOTE: below is a rewrite of the event handler (above) using a `path` Interceptor
;; You'll find it illuminating to compare this rewrite with the original.
;;
;; A `path` interceptor has BOTH a before and after action.
;; When you create one, you supply "a path" into `app-db`, like:
;; [:a :b 1]
;; The job of "before" is to replace the app-db with the value
;; of `app-db` at the nominated path. And, then, "after" to
;; take the event handler returned value and place it back into
;; app-db at the nominated path.  So the event handler works
;; with a particular, narrower path within app-db, not all of it.
;;
;; So, `path` operates a little like `update-in`
;;
; #_(reg-event-db
;   :set-showing

;   ;; this now a chain of 2 interceptors. Note use of `path`
;   [check-spec-interceptor (path :showing)]

;   ;; The event handler
;   ;; Because of the `path` interceptor above, the 1st parameter to
;   ;; the handler below won't be the entire 'db', and instead will
;   ;; be the value at the path `[:showing]` within db.
;   ;; Equally the value returned will be the new value for that path
;   ;; within app-db.
;   (fn [old-showing-value [_ new-showing-value]]
;     new-showing-value))                  ;; return new state for the path


;; usage:  (dispatch [:add-todo  "a description string"])
(reg-event-db                     ;; given the text, create a new todo
  :add-entry
  todo-interceptors
  (fn [entries [_ text]]
    (let [id (allocate-next-id entries)]
      (assoc entries id {:id id :text text}))))

; (reg-event-db
;   :toggle-done
;   todo-interceptors
;   (fn [todos [_ id]]
;     (update-in todos [id :done] not)))


; (reg-event-db
;   :save
;   todo-interceptors
;   (fn [todos [_ id title]]
;     (assoc-in todos [id :title] title)))


; (reg-event-db
;   :delete-todo
;   todo-interceptors
;   (fn [todos [_ id]]
;     (dissoc todos id)))


; (reg-event-db
;   :clear-completed
;   todo-interceptors
;   (fn [todos _]
;     (let [done-ids (->> (vals todos)         ;; which todos have a :done of true
;                         (filter :done)
;                         (map :id))]
;       (reduce dissoc todos done-ids))))      ;; delete todos which are done


; (reg-event-db
;   :complete-all-toggle
;   todo-interceptors
;   (fn [todos _]
;     (let [new-done (not-every? :done (vals todos))]   ;; work out: toggle true or false?
;       (reduce #(assoc-in %1 [%2 :done] new-done)
;               todos
;               (keys todos)))))
