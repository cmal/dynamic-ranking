(ns ^:figwheel-no-load dynamic-ranking.app
  (:require [dynamic-ranking.core :as core]
            [devtools.core :as devtools]))

(enable-console-print!)

(devtools/install!)

(core/init!)
