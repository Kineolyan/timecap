(ns timecap.form.entry
  (:require 
    [re-frame.core :refer [subscribe dispatch]]
    [timecap.form.cpns :refer [controlled-input]]))


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

(defn new-entry-form [_]
  (let [content (subscribe [:new-form])]
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
