(defproject babylone "0.1.0-SNAPSHOT"
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/clojurescript "1.9.293"]
                 [org.clojure/core.async "0.2.395"]
                 [reagent "0.6.0"]
                 [re-frame "0.8.0"]
                 ;; [day8.re-frame/async-flow-fx "0.0.6"]
                 [secretary "1.2.3"]
                 [compojure "1.5.0"]
                 [yogthos/config "0.8"]
                 [ring "1.4.0"]
                 ;; [cljsjs/babylon "2.2.0-0"]
                 ]

  :plugins [[lein-cljsbuild "1.1.4"]]

  :min-lein-version "2.5.3"

  :source-paths ["src/clj"]

  :clean-targets ^{:protect false} ["resources/public/js/compiled" "target"
                                    "test/js"]

  :figwheel {:css-dirs ["resources/public/css"]
             :ring-handler babylone.handler/dev-handler
             
             ;; Start an nREPL server into the running figwheel process             
             :nrepl-port 7888

             :nrepl-middleware ["cemerick.piggieback/wrap-cljs-repl"
                                "cider.nrepl/cider-middleware"
                                "refactor-nrepl.middleware/wrap-refactor"]}

  :repl-options {:nrepl-middleware [refactor-nrepl.middleware/wrap-refactor
                                    cemerick.piggieback/wrap-cljs-repl]}

  :profiles
  {:dev
   {:dependencies [[binaryage/devtools "0.8.2"]
                   [figwheel-sidecar "0.5.8"]
                   [com.cemerick/piggieback "0.2.1"]]

    :plugins      [[lein-figwheel "0.5.8"]
                   [lein-doo "0.1.7"]
                   [cider/cider-nrepl "0.14.0"]
                   [org.clojure/tools.namespace "0.3.0-alpha2"
                    :exclusions [org.clojure/tools.reader]]
                   [refactor-nrepl "2.3.0-snapshot"
                    :exclusions [org.clojure/clojure]]]}}

  :cljsbuild
  {:builds
   [{:id           "dev"
     :source-paths ["src/cljs"]
     :figwheel     {:on-jsload "babylone.core/mount-root"}
     :compiler     {:main                 babylone.core
                    :output-to            "resources/public/js/compiled/app.js"
                    :output-dir           "resources/public/js/compiled/out"
                    :asset-path           "js/compiled/out"
                    :source-map-timestamp true
                    :preloads             [devtools.preload]
                    ;; to use latest babylon release
                    :externs ["babylon.ext.js"]
                    :foreign-libs [{:file "resources/public/babylon.js"
                                    :provides ["cljsjs.babylon"]}]
                    :external-config      {:devtools/config {:features-to-install :all}}
                    }}

    {:id           "min"
     :source-paths ["src/cljs"]
     :jar true
     :compiler     {:main            babylone.core
                    :output-to       "resources/public/js/compiled/app.js"
                    :optimizations   :advanced
                    :closure-defines {goog.DEBUG false}
                    :pretty-print    false}}

    {:id           "test"
     :source-paths ["src/cljs" "test/cljs"]
     :compiler     {:main          babylone.runner
                    :output-to     "resources/public/js/compiled/test.js"
                    :output-dir    "resources/public/js/compiled/test/out"
                    :optimizations :none}}
    ]}

  :main babylone.server

  :aot [babylone.server]

  :uberjar-name "babylone.jar"

  :prep-tasks [["cljsbuild" "once" "min"] "compile"]
  )
