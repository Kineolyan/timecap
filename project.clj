(defproject time-cap "0.1.0-SNAPSHOT"
  :dependencies [
                  [org.clojure/clojure "1.9.0"]
                  [reagent "0.8.1"]
                  [re-frame "0.10.6"]
                  [binaryage/devtools "0.9.10"]
                  [day8.re-frame/tracing "0.5.1"]
                  [clj-commons/secretary "1.2.4"]
                  [com.andrewmcveigh/cljs-time "0.5.2"]]
  :profiles
    {:dev
      {:dependencies [[org.clojure/clojurescript "1.10.339"]
                      [com.bhauman/figwheel-main "0.2.0"]
                      ;; optional but recommended
                      [com.bhauman/rebel-readline-cljs "0.1.4"]]}}
  :aliases {
            "fig" ["trampoline" "run" "-m" "figwheel.main"]
            "dev" ["trampoline" "run" "-m" "figwheel.main" "--build" "dev" "--repl"]}
  :resource-paths ["target" "resources"])
