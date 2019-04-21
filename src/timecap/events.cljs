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
                        ->local-store
                        (path :entries)])

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
; Special function to test if the stored content a version.
; Given that the stored content is unknown, its structure may not be a map as expected
(defn get-stored-version
  [content]
  (cond
    (not (map? content)) nil
    (not (some #{:version} (keys content))) nil
    :else (:version content)))
(defn integrate-local-storage
  [current-content stored-content]
  (let [ stored-version (get-stored-version stored-content)]
    (if (= timecap.db/app-version stored-version)
        {:db (assoc current-content :entries (get-in stored-content [:db :entries]))}
        ; drop the stored content
        {:db current-content})))
(reg-event-fx  
  :initialise-db
  [
    (inject-cofx :local-store-timecap)
    check-spec-interceptor]
  (fn [{:keys [local-store-timecap]} _] (integrate-local-storage default-db local-store-timecap)))


;; usage:  (dispatch [:add-todo  "a description string"])
(reg-event-db
  :add-entry
  todo-interceptors
  (fn [entries [_ text]]
    (let [
            id (timecap.db/generate-id)
            t-id (timecap.db/generate-id)]
      (assoc 
        entries 
        id 
        {
          :id id 
          :text text 
          :date "12/04/2027"
          :timeline-id t-id
          :edition-date "05/04/2019"}))))

(reg-event-db
  :delete-entry
  todo-interceptors
  (fn [entries [_ id]]
    (dissoc entries id)))

; (reg-event-db
;   :toggle-done
;   todo-interceptors
;   (fn [todos [_ id]]
;     (update-in todos [id :done] not)))

