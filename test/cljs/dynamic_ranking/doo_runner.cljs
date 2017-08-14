(ns dynamic-ranking.doo-runner
  (:require [doo.runner :refer-macros [doo-tests]]
            [dynamic-ranking.core-test]))

(doo-tests 'dynamic-ranking.core-test)

