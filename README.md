# re-frame Game of Life

A [re-frame](https://github.com/Day8/re-frame) front-end to visualize and interact
with Conway Game of Life ([Wikipedia](https://en.wikipedia.org/wiki/Conway%27s_Game_of_Life)).

The aim of this application is to explore re-frame and learn how code in
re-frame should be organized and structured. For this purpose, the following
interactions are added to the application:

* Run simulation
* Pause simulation
* Step through simulation
* Edit interval between each generation
* Edit the seed of the simulation

re-frame uses [Reagent](https://github.com/reagent-project/reagent), so this
application is also used to experiment with Reagent components to display
Game of Life on [HTML5 SVG](https://www.w3schools.com/Html/html5_svg.asp) and 
on [HTML5 Canvas](https://www.w3schools.com/Html/html5_canvas.asp).  
 
See live demo [here](https://saicheong.github.io/reframe-game-of-life/index.html).

## Development Mode

### Run application:

```
lein clean
lein figwheel dev
```

Figwheel will automatically push cljs changes to the browser.

Wait a bit, then browse to [http://localhost:3449](http://localhost:3449).

## Production Build

To compile clojurescript to javascript:

```
lein clean
lein cljsbuild once min
```

> __NOTE__: This version uses a [quick fix to resolve an extern issue](https://clojureverse.org/t/error-when-using-advance-compiler-optimization-createsvgpoint-is-not-a-function/2330?u=saicheong) 
by avoiding javascript interop for an externally defined type (SVG Element). 
See [here](https://github.com/saicheong/reframe-game-of-life/tree/shadow-cljs) for 
an alternative solution using [Shadow CLJS](https://github.com/thheller/shadow-cljs)
which can generate extern file so that you do not have to avoid or forgo
standard javascript interop.    

## License

Copyright © 2018 Yow Sai Cheong

Released under the MIT license.