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

## Using Shadow CLJS as the ClojureScript compiler
The [Shadow CLJS](http://shadow-cljs.org) compiler is used to compile
ClojureScript to Javascript. If you are new to Shadow CLJS, this 
[browser example](https://github.com/shadow-cljs/quickstart-browser)
should get you started on using it for compiling ClojureScript for the browser.

`shadow-cljs` is used here to deal with
[externs](https://developers.google.com/closure/compiler/docs/api-tutorial3). 
Shadow CLJS can warn about extern issues and generate the appropriate externs
on objects with `^js` type hints added. 

You can read more about how `shadow-cljs` deals with externs in `shadow-cljs`
[user manual](https://shadow-cljs.github.io/docs/UsersGuide.html#externs).
The author of `shadow-cljs` also has a 
[blog article](https://code.thheller.com/blog/shadow-cljs/2017/10/15/externs-the-bane-of-every-release-build.html)
on this issue.

## Development Mode

```bash
git clone https://github.com/saicheong/reframe-game-of-life.git conway
cd conway
npm install
npx shadow-cljs server
```
This run the `shadow-cljs` server process which all following commands will talk
to. Just leave it running and open a new terminal to continue.
 
```bash
npx shadow-cljs watch app
```

This will begin the compilation of the configured `:app` build and re-compile 
whenever you change a file.

As this application uses react, the compile will fail if react, react-dom and
create-react-class are not available. Run the following to install any missing
dependencies.

```bash
npm install react react-dom create-react-class
```

When you see a "Build completed." message, your build is ready to be used.
You can now open [http://localhost:8020](http://localhost:8020)


## Production Build

To compile clojurescript to javascript:

```bash
npx shadow-cljs release app
```

## License

Copyright © 2018 Yow Sai Cheong

Released under the MIT license.