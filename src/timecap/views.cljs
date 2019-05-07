(ns timecap.views
  (:require 
    [reagent.core  :as r]
    [re-frame.core :refer [subscribe dispatch]]
    [timecap.form.entry :as f-entry]
    [timecap.form.timeline :as f-timeline]))


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


(defn entry-list
  []
  (let [entries @(subscribe [:entries])]
      [:section#main
        [:ul#todo-list
          (for [entry entries]
            ^{:key (:id entry)} [entry-item entry])]]))


(defn app
  []
  [:div
    [:div
      [f-timeline/new-timeline-form
        {:id "new-timeline"}]
      [f-entry/new-entry-form
        {:id "new-entry"}]]
    (entry-list)])
