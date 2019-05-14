(ns timecap.form.timeline
  (:require 
    [re-frame.core :refer [subscribe dispatch]]
    [clojure.string :as str]
    [timecap.form.cpns :refer [controlled-input]]))

(defn submit-new-timeline
  [e]
  (.preventDefault e)
  (dispatch [:submit-new-timeline]))
(defn new-timeline-form
  [_]
  (let [content (subscribe [:timeline-form])]
    (fn [_]
      [:form#new-timeline
        {:on-submit submit-new-timeline}
        [controlled-input
          { :placeholder "Name of the timeline"
            :value (get-in @content [:timeline :name])
            :on-change #(dispatch [:update-timeline-name %])}]
        [:button
          { 
            :type "submit"}
          "Submit"]])))
    
