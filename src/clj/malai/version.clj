(ns malai.version
  "malai's version and the upstream Specter it is based on. Kept out of
   malai.core so :refer :all on malai.core takes only Specter's names.")

(def version "malai's version." "0.3.0")

(def specter-base
  "The upstream Specter version and commit malai is based on."
  "1.1.7-SNAPSHOT@6119462")
