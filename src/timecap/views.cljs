(ns timecap.views
  (:require [reagent.core  :as r]
            [re-frame.core :refer [subscribe dispatch]]
            [clojure.string :as str]))

(defn entry-input [{:keys [title on-save on-stop]}]
  (let [val  (r/atom title)
        stop #(do (reset! val "")
                  (when on-stop (on-stop)))
        save #(let [v (-> @val str str/trim)]
                (on-save v)
                (stop))]
    (fn [props]
      [:input (merge (dissoc props :on-save :on-stop :title)
                     {:type        "text"
                      :value       @val
                      :auto-focus  true
                      :on-blur     save
                      :on-change   #(reset! val (-> % .-target .-value))
                      :on-key-down #(case (.-which %)
                                      13 (save)
                                      27 (stop)
                                      nil)})])))
  

(defn create-complete-entry
  [content date]
  ; no need for a cas loop for CLJS, single threaded
  (let [c @content d @date]
    (when (and (not (str/blank? c)) (not (str/blank? d)))
      (do
        (dispatch :add-entry c d)
        (reset! content "")
        (reset! date "")))))


(defn new-entry-form [{:keys [on-save on-cancel]}]
  (let [content (r/atom "")
        target-date (r/atom "")
        do-save (fn [_] create-complete-entry content target-date)]
    (fn [props]
      ^{:key (:id props)}
      [:div
        [entry-input
          { :placeholder "What do you think about the future?"
            :on-save do-save}]
        [entry-input
          { :placeholder "Target date"
            :on-save do-save}]
        [:button 
          {:on-click #(do-save nil)}
          "Submit"]])))


(defn entry-item
  []
  (fn [{:keys [id text date edition-date]}]
    [:li
      [:div.view
        [:div
          [:label text]
          [:button.destroy
            {:on-click #(dispatch [:delete-entry id])}
            "-"]]
        [:div
          [:label (str "Target date: " date)]]
        [:div
          [:i (str "(edited: " edition-date ")")]]]]))


(defn task-list
  []
  (let [entries @(subscribe [:entries])]
      [:section#main
        [:ul#todo-list
          (for [entry entries]
            ^{:key (:id entry)} [entry-item entry])]]))


; (defn footer-controls
;   []
;   (let [[active done] @(subscribe [:footer-counts])
;         showing       @(subscribe [:showing])
;         a-fn          (fn [filter-kw txt]
;                         [:a {:class (when (= filter-kw showing) "selected")
;                              :href (str "#/" (name filter-kw))} txt])]
;     [:footer#footer
;      [:span#todo-count
;       [:strong active] " " (case active 1 "item" "items") " left"]
;      [:ul#filters
;       [:li (a-fn :all    "All")]
;       [:li (a-fn :active "Active")]
;       [:li (a-fn :done   "Completed")]]
;      (when (pos? done)
;        [:button#clear-completed {:on-click #(dispatch [:clear-completed])}
;         "Clear completed"])]))


(defn app
  []
  [:div
    [:div
      [new-entry-form
        {:id "new-entry"
          :on-save #(when (seq %) (dispatch [:add-entry %]))}]]
    (task-list)])
