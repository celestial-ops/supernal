(comment
  Celestial, Copyright 2017 Ronen Narkis, narkisr.com
  Licensed under the Apache License,
  Version 2.0  (the "License") you may not use this file except in compliance with the License.
  You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.)

(ns supernal.repl
  "Repl utilities for supernal"
  (:require
    [taoensso.timbre :refer (refer-timbre set-level!)]
    [supernal.repl.base :refer (refer-base)]
    [supernal.repl.output :refer (refer-out)]
    [supernal.repl.stats :refer (idle)])
  (:import [supernal.repl.base Hosts]) 
  )

(refer-base)
(refer-out)

(def hosts (Hosts. {:user "vagrant"} ["192.168.2.25" "192.168.2.26" "192.168.2.27"]))

(run (initialize hosts) | (ping "google.com") | (log-) | (ls "/home/vagrant/" "-la") | (log-))
;; (run (initialize hosts) | (idle) | (pretty))
