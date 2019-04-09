(ns timecap.views
  (:require [reagent.core  :as reagent]
            [re-frame.core :refer [subscribe dispatch]]
            [clojure.string :as str]))

(defn new-entry-input [{:keys [title on-save on-stop]}]
  (let [val  (reagent/atom title)
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


; (defn task-entry
;   []
;   [:header#header
;     [:h1 "todos"]
;     [todo-input
;       {:id "new-todo"
;        :placeholder "What needs to be done?"
;        :on-save #(when (seq %)
;                     (dispatch [:add-todo %]))}]])


(defn app
  []
  [:div
    [:div
      [new-entry-input
        {:id "new-entry"
          :placeholder "Your thought about the future?"
          :on-save #(when (seq %) (dispatch [:add-entry %]))}]]
    (task-list)])
