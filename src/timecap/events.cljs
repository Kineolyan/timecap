(ns timecap.events
  (:require
    [timecap.db    :as db :refer [default-db timecap->local-store generate-id]]
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
(def ->local-store-interceptor (after timecap->local-store))

;; -- Interceptor Chain ------------------------------------------------------
(def todo-interceptors 
  [
    check-spec-interceptor
    ->local-store-interceptor
    (path :entries)])

(def form-interceptors 
  [
    check-spec-interceptor
    (path :new-form)])

(def timeline-form-interceptors
  [
    check-spec-interceptor
    (path :timeline-form)])

(def submit-interceptors
  [
    check-spec-interceptor
    ->local-store-interceptor])
  

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
(defn get-stored-version
  [content]
  (cond
    (not (map? content)) nil
    (not (some #{:version} (keys content))) nil
    :else (:version content)))
(defn integrate-local-storage
  [current-content stored-content]
  (let [ stored-version (get-stored-version stored-content)]
    (case stored-version
      3 
      {:db (assoc 
              current-content 
              :entries 
              (get-in stored-content [:db :entries]))}
      4
      {:db (assoc
              current-content
              :entries
              (get-in stored-content [:db :entries])
              :timelines
              (get-in stored-content [:db :timelines]))}
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

(reg-event-db
  :update-form-text
  form-interceptors
  (fn [content [_ text]]
    (assoc content :text text)))
(reg-event-db
  :update-form-date
  form-interceptors
  (fn [content [_ date]]
    (assoc content :date date)))
(reg-event-db
  :update-entry-timeline
  form-interceptors
  (fn [content [_ id]]
    (assoc content :timeline-id id)))

(defn extract-new-entry
  [db]
  (let [
        {:keys [text date]} (:new-form db)]
    (assoc 
      (get-in db [:new-form])
      :id (generate-id #(db/is-valid-new-entry-id? db %))
      :timeline-id (generate-id #(db/is-valid-new-timeline-id? db %))
      :text (get-in db [:new-form :text])
      :edition-date "17/05/2019")))

(defn commit-new-entry
  [db]
  (let [new-entry (extract-new-entry db)]
    (-> 
      db
      (db/add-entry new-entry)
      (db/reset-form))))      

(reg-event-db
  :submit-new-entry
  submit-interceptors
  commit-new-entry)

(reg-event-db
  :update-timeline-name
  timeline-form-interceptors
  (fn [content [_ value]]
    (assoc content :name value)))

(defn extract-new-timeline
  [db]
  (let [{:keys [name]} (:timeline-form db)]
    {
      :id (generate-id #(db/is-valid-new-timeline-id? db %))
      :name name}))

(defn commit-new-timeline
  [db]
  (let [new-timeline (extract-new-timeline db)]
    (->
      db
      (db/add-timeline new-timeline)
      (db/reset-timeline-form))))

(reg-event-db
  :submit-new-timeline
  submit-interceptors
  commit-new-timeline)
