# Ergo 

Learning ergodicity in economics using examples.

> Ergodicity means the ensemble average equals the time average.  

## Peters coin game (additive)

Suppose we have a gamble that has additive dynamics. Win 5$ for heads, lose 4$ for tails.

```sh
out/ergo/native/dest/ergo -m additive --initial 100 --win 5 --lose 4 | ./plot-it
``` 


## Peters coin game (multiplicative)
Now consider a gamble with multiplicative dynamics.

Win 50% of your current wealth for heads, lose 40% of your current wealth for tails.

```sh
out/ergo/native/dest/ergo -m multiplicative | ./plot-it
``` 

### Dependencies
Install Mill http://www.lihaoyi.com/mill/
Compiling it with Graal VM following the same approach as https://github.com/jonasanso/hello-world-scala-graalvm 
Install jplot https://github.com/rs/jplot 

### Reads
- Original game: https://twitter.com/hulme_oliver/status/1139148255969906689
- Ergodicity economics https://ergodicityeconomics.com/lecture-notes/
- General ergodicity (broader than economics) - https://www.wikiwand.com/en/Ergodicity  
- Clear contrast with https://www.wikiwand.com/en/Expected_utility_hypothesis 




