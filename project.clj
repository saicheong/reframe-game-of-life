(defproject conway "0.1.0-SNAPSHOT"
  :dependencies [[org.clojure/clojure "1.9.0"]
                 [org.clojure/clojurescript "1.10.238"]
                 [reagent "0.7.0"]
                 [re-frame "0.10.5"]]
  :source-paths ["src/clj"]
  :min-lein-version "2.5.3"

  :profiles
  {:cljs
   {:source-paths  ["src/cljs"]
    :dependencies  [[thheller/shadow-cljs "2.4.12"]
                    [binaryage/devtools "0.9.10"]]
    :clean-targets ^{:protect false} ["resources/public/js/compiled" "target"]}}

  )