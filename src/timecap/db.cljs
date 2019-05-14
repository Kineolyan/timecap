(ns timecap.db
  (:require [cljs.reader]
            [cljs.spec.alpha :as s]
            [re-frame.core :as re-frame]
            [goog.string :as jstrings]))

;; -- Spec --------------------------------------------------------------------
;;
;; This is a clojure.spec specification for the value in app-db. It is like a
;; Schema. See: http://clojure.org/guides/spec

; (s/def ::id int?)
; (s/def ::title string?)
; (s/def ::done boolean?)
; (s/def ::todo (s/keys :req-un [::id ::title ::done]))
; (s/def ::todos (s/and                                       ;; should use the :kind kw to s/map-of (not supported yet)
;                  (s/map-of ::id ::todo)                     ;; in this map, each todo is keyed by its :id
;                  #(instance? PersistentTreeMap %)           ;; is a sorted-map (not just a map)
;                  ))
; (s/def ::showing                                            ;; what todos are shown to the user?
;   #{:all                                                    ;; all todos are shown
;     :active                                                 ;; only todos whose :done is false
;     :done                                                   ;; only todos whose :done is true
;     })
; (s/def ::db (s/keys :req-un [::todos ::showing]))
(defn generated-id?
  [value]
  ; TODO implement a real check for xxxx-xxxx-xxxx-xxxx
  (string? value))
(defn a-date? 
  [value]
  (string? value))
(s/def ::id string?)
(s/def ::text string?)
(s/def ::date a-date?)
(s/def ::edition-date a-date?)
(s/def ::timeline-id string?)
(s/def ::name string?)

(s/def ::timeline (s/keys :req-un [::id ::name]))
(s/def ::timelines (s/map-of ::id ::timeline))

(s/def ::entry (s/keys :req-un [::id ::text ::date ::timeline-id ::edition-date]))
(s/def ::entries (s/map-of ::id ::entry))

(s/def ::new-form (s/keys :req-un [::text ::date] :opt [::timeline-id]))
(s/def ::timeline-form (s/keys :req-un [::name]))
(s/def ::db (s/keys :req-un [
                              ::entries  
                              ::new-form 
                              ::timelines
                              ::timeline-form]))

;; -- Default app-db Value  ---------------------------------------------------
;;
;; When the application first starts, this will be the value put in app-db
;; Unless, of course, there are todos in the LocalStore (see further below)
;; Look in:
;;   1.  `core.cljs` for  "(dispatch-sync [:initialise-db])"
;;   2.  `events.cljs` for the registration of :initialise-db handler
;;
(def generation-seed (atom 0))
(defn generate-id
  (
    []
    (let [id (swap! generation-seed inc)]
      (jstrings/format "abcd-1234-xyza-%04d" id)))
  (
    [is-valid?]
    (loop [id (generate-id)]
      (if 
        (is-valid? id)
        id
        (recur (generate-id))))))
  

(def default-db
  (let [first-id (generate-id)
        main-timeline (generate-id)]
    {
      :timeline-form 
      {
        :name ""}
      :new-form
      { 
        :text "Be the world leader"
        :date "12/04/2017"}
      :timelines
      {
        main-timeline
        {
          :id main-timeline
          :name "<main>"}}
      :entries 
      {
        first-id
        {
          :id first-id
          :text "Quand je serai vieux, Je ne serai pas chiant."
          :date "13/09/2048"
          :timeline-id main-timeline
          :edition-date "24/03/2019"}}}))

;; -- Db operations -------------------------------------------

(defn is-valid-new-entry-id?
  [db id]
  (not
    (contains? (:entries db) id)))
(defn is-valid-new-timeline-id?
  [db id]
  (not 
    (contains? (:timelines db) id)))

(defn index-by-id [l entry] (assoc l (:id entry) entry))

(defn add-entry
  [db entry]
  (update-in db [:entries] #(index-by-id % entry)))

(defn reset-form
  (
    [db]
    (reset-form db {:text "" :date ""})) 
  (
    [db reset-value]
    (assoc db :new-form reset-value)))

(defn add-timeline
  [db entry]
  (update-in db [:timelines] #(index-by-id % entry)))

(defn reset-timeline-form
  (
    [db]
    (reset-timeline-form db {:name ""})) 
  (
    [db reset-value]
    (assoc db :timeline-form reset-value)))

;; -- Local Storage  ----------------------------------------------------------

(def app-version 4)
(def ls-key "timecap-reframe")
(defn timecap->local-store
  "Puts time-cap into localStorage"
  [db]
  (.setItem 
    js/localStorage 
    ls-key 
    (str 
      {
        :db (select-keys db [:entries :timelines])
        :version app-version})))

;; -- cofx Registrations  -----------------------------------------------------

;; Use `reg-cofx` to register a "coeffect handler" which will inject the todos
;; stored in localstore.
;;
;; To see it used, look in `events.cljs` at the event handler for `:initialise-db`.
;; That event handler has the interceptor `(inject-cofx :local-store-timecap)`
;; The function registered below will be used to fulfill that request.
;;
;; We must supply a `sorted-map` but in LocalStore it is stored as a `map`.
;;
(defn local-store->timecap
  "Reads time-cap from the localStorage"
  []
  (into 
    (sorted-map)
    (some->> 
      (.getItem js/localStorage ls-key)
      (cljs.reader/read-string))))
  
(re-frame/reg-cofx
  :local-store-timecap
  (fn [cofx _]
      ;; put the localstore todos into the coeffect under :local-store-timecap
      (assoc cofx :local-store-timecap (local-store->timecap))))

