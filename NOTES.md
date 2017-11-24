# Notes

These are some notes for the language.

## Function Declaration/Definition

These are going to be separate like Haskell. This will simplify the
syntax and make external function declarations simple.

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

