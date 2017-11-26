# Notes

These are some notes for the language.

## Function Declaration/Definition

These are going to be separate like Haskell. This will simplify the
syntax and make external function declarations simple to parse.

```Haskell
add :: Int -> Int -> Int
add x y = x + y
```

## Side Effects are Marked

Side Effects like I/O need to be marked in the function declaration.

```Haskell
import io

-- The compiler would error if +IO wasn't there.

println :: Text -> Text +IO
println x = print (x ++ "\n")
```

This will allow us to determine whether functions are pure, which we
could optimize somehow. This will also give the mindset that "pure code
is safe code."

## Modules

Modules will export every function and inside them (private functions
can be dealt with later).

```
// in test.f

add :: Int -> Int -> Int
add x y = x + y.

addThree :: Int -> Int -> Int -> Int
addThree x y z =
  let w = add x y,
  add w z.
```

```
// in test2.f

import test

addFour w x y z =
  let d = addThree x y z,
  add d w.
```

## Traits & Structs
There is no subtyping. Structs hold data and Traits define functions on
that data.

```
trait Accumulator {
  inc :: Int -> Int
}

struct IntBox : Accumulator {
  let x : Int            // Immutable
  var acc : Int          // Mutable

  inc :: Int -> Int      // Can be inferred
  inc x =                // Compiles to: int inc(IntBox* this, int x)
    this.x = this.x + 1, // Modifies struct
    this.x.
}
```

## Infix Operators
Infix operators will be converted to function calls. This was something
I didn't think about in the last compiler until it was too late.

