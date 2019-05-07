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


(defn controlled-input [props]
  (let [{:keys [value on-change]} props
        other-props (dissoc props :on-change :value)]
    [:input
      (merge
        other-props
        {
          :type        "text"
          :value       value
          :on-change   #(on-change (-> % .-target .-value))})]))


; (defn create-complete-entry
;   [content date]
;   ; no need for a cas loop for CLJS, single threaded
;   (let [c @content d @date]
;     (when (and (not (str/blank? c)) (not (str/blank? d)))
;       (do
;         (dispatch :add-entry c d)
;         (reset! content "")
;         (reset! date "")))))
(defn submit-new-form
  [e]
  (do
    (.preventDefault e)
    (dispatch [:submit-new-entry])))
(defn form-submit-props
  [entry]
  (let [disabled-props (if (:valid? entry) {} {:disabled "disabled"})]
    (assoc 
      disabled-props 
      :type "submit"
      :on-click submit-new-form)))

(defn new-entry-form [{:keys [on-save on-cancel]}]
  (let [content (subscribe [:new-form])
        do-save (fn [_] nil)]
    (fn [props]
      ^{:key (:id props)}
      [:form#new-entry
        {:on-submit submit-new-form}
        [controlled-input
          { :placeholder "What do you think about the future?"
            :value (get-in @content [:entry :text])
            :on-change #(dispatch [:update-form-text %])}]
        [controlled-input
          { :placeholder "Target date"
            :value (get-in @content [:entry :date])
            :on-change #(dispatch [:update-form-date %])}]
        [:button
          (form-submit-props @content)
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
