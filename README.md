# re-sponza

A [re-frame](https://github.com/Day8/re-frame) application.

### Run application:

```
lein clean
lein figwheel dev
```

Figwheel will automatically push cljs changes to the browser.

Wait a bit, then browse to [http://localhost:3449](http://localhost:3449).

To compile clojurescript to javascript:

```
lein clean
lein cljsbuild once min
```

##TODO

* Try [re-frame-async-flow-fx](https://github.com/Day8/re-frame-async-flow-fx)
* Loading Panel
* Sound Support