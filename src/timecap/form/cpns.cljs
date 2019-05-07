(ns timecap.form.cpns)
    ; [clojure.string :as str]

; (defn entry-input [{:keys [title on-save on-stop]}]
;   (let [val  (r/atom title)
;         stop #(do (reset! val "")
;                   (when on-stop (on-stop)))
;         save #(let [v (-> @val str str/trim)]
;                 (on-save v)
;                 (stop))]
;     (fn [props]
;       [:input (merge (dissoc props :on-save :on-stop :title)
;                      {:type        "text"
;                       :value       @val
;                       :auto-focus  true
;                       :on-blur     save
;                       :on-change   #(reset! val (-> % .-target .-value))
;                       :on-key-down #(case (.-which %)
;                                       13 (save)
;                                       27 (stop)
;                                       nil)})])))

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
