# codename `f2`

> `f2` is the placeholder name for this language until I think of a
> cooler name.

This is a low-level language with _compile time memory management_ that
will soon be viable for general purpose development, with a focus on
portability.

The main idea this language is pushing is that memory can be managed at
_compile time._ We'll see how feasible this as we go.

Currently, the language is parsed into an AST using ANTLR, translated
into an IR, and that IR is converted to LLVM IR.

The syntax is influenced by Haskell, C, Prolog, and everything else out
there. Here's what the language can support:

```haskell
struct X {
  a : Int32
}

struct Y {
  x : X
}

f :: X -> Int32
f x = x.a.

g :: Int32 -> X
g a = X{a}.

h :: Int32 -> Int32
h a = let x = X{a},
      x.a.

i :: Int32 -> Y
i a = Y{X{a}}.

j :: Int32 -> Int32
j a = let x = X{a},
      let y = Y{x},
      let w = y.x,
      w.a.
```

This has some functions that include heap and stack allocations.
This is translated into IR, which can be printed out:

```
fun f(%0 : X) : Int32 ()
    FieldGetInstruction(debugInfo=(14,6), registerIndex=0, fieldIndex=0)
    StoreInstruction(debugInfo=(14,6), register=1)
    ReturnInstruction(debugInfo=(14,6), registerIndex=1)

fun g(%0 : Int32) : X ()
    HeapAllocateInstruction(debugInfo=(17,6), type=X)
    StoreInstruction(debugInfo=(17,6), register=1)
    FieldSetInstruction(debugInfo=(17,8), structRegisterIndex=1, fieldIndex=0, valueRegisterIndex=0)
    ReturnInstruction(debugInfo=(17,6), registerIndex=1)

fun h(%0 : Int32) : Int32 ()
    StackAllocateInstruction(debugInfo=(20,14), type=X)
    StoreInstruction(debugInfo=(20,14), register=1)
    FieldSetInstruction(debugInfo=(20,16), structRegisterIndex=1, fieldIndex=0, valueRegisterIndex=0)
    FieldGetInstruction(debugInfo=(21,6), registerIndex=1, fieldIndex=0)
    StoreInstruction(debugInfo=(21,6), register=2)
    ReturnInstruction(debugInfo=(21,6), registerIndex=2)

fun i(%0 : Int32) : Y ()
    HeapAllocateInstruction(debugInfo=(24,8), type=X)
    StoreInstruction(debugInfo=(24,8), register=1)
    FieldSetInstruction(debugInfo=(24,10), structRegisterIndex=1, fieldIndex=0, valueRegisterIndex=0)
    HeapAllocateInstruction(debugInfo=(24,6), type=Y)
    StoreInstruction(debugInfo=(24,6), register=2)
    FieldSetInstruction(debugInfo=(24,8), structRegisterIndex=2, fieldIndex=0, valueRegisterIndex=1)
    ReturnInstruction(debugInfo=(24,6), registerIndex=2)

fun j(%0 : Int32) : Int32 ()
    StackAllocateInstruction(debugInfo=(27,14), type=X)
    StoreInstruction(debugInfo=(27,14), register=1)
    FieldSetInstruction(debugInfo=(27,16), structRegisterIndex=1, fieldIndex=0, valueRegisterIndex=0)
    StackAllocateInstruction(debugInfo=(28,14), type=Y)
    StoreInstruction(debugInfo=(28,14), register=2)
    FieldSetInstruction(debugInfo=(28,16), structRegisterIndex=2, fieldIndex=0, valueRegisterIndex=1)
    FieldGetInstruction(debugInfo=(29,14), registerIndex=2, fieldIndex=0)
    StoreInstruction(debugInfo=(29,6), register=3)
    FieldGetInstruction(debugInfo=(30,6), registerIndex=3, fieldIndex=0)
    StoreInstruction(debugInfo=(30,6), register=4)
    ReturnInstruction(debugInfo=(30,6), registerIndex=4)
```

The IR is register (& stack?) based, which translates easily to
LLVM IR:

```llvm
define i32 @f(%X*) {
entry:
  %1 = getelementptr inbounds %X, %X* %0, i32 0, i32 0
  %2 = load i32, i32* %1
  ret i32 %2
}

define %X* @g(i32) {
entry:
  %1 = call i8* @malloc(i64 4)
  %2 = bitcast i8* %1 to %X*
  %3 = getelementptr inbounds %X, %X* %2, i32 0, i32 0
  store i32 %0, i32* %3
  ret %X* %2
}

define i32 @h(i32) {
entry:
  %1 = alloca %X
  %2 = getelementptr inbounds %X, %X* %1, i32 0, i32 0
  store i32 %0, i32* %2
  %3 = getelementptr inbounds %X, %X* %1, i32 0, i32 0
  %4 = load i32, i32* %3
  ret i32 %4
}

define %Y* @i(i32) {
entry:
  %1 = call i8* @malloc(i64 4)
  %2 = bitcast i8* %1 to %X*
  %3 = getelementptr inbounds %X, %X* %2, i32 0, i32 0
  store i32 %0, i32* %3
  %4 = call i8* @malloc(i64 8)
  %5 = bitcast i8* %4 to %Y*
  %6 = getelementptr inbounds %Y, %Y* %5, i32 0, i32 0
  store %X* %2, %X** %6
  ret %Y* %5
}

define i32 @j(i32) {
entry:
  %1 = alloca %X
  %2 = getelementptr inbounds %X, %X* %1, i32 0, i32 0
  store i32 %0, i32* %2
  %3 = alloca %Y
  %4 = getelementptr inbounds %Y, %Y* %3, i32 0, i32 0
  store %X* %1, %X** %4
  %5 = getelementptr inbounds %Y, %Y* %3, i32 0, i32 0
  %6 = load %X*, %X** %5
  %7 = getelementptr inbounds %X, %X* %6, i32 0, i32 0
  %8 = load i32, i32* %7
  ret i32 %8
}
```

Heap and stack allocations are properly translated to `malloc` and
`alloca`. This part is easy. The more challenging part is knowing
_when_ to free this memory. Here's an example of one way we're
freeing memory at compiletime:

```haskell
struct X {
  a : Int32
}

box :: Int32 -> X
box a = X{a}. -- memory is allocated here

f :: Int32 -> Int32
f i = let x = box(i),
      let a = firstThing(x),
      let b = secondThing(x),
      let c = thirdThing(x), -- memory can be freed after this call
      a + b + c.
```

This is translated to:

```llvm
define %X* @box(i32) {
entry:
  %1 = call i8* @malloc(i64 4) ; memory allocated on the heap here
  %2 = bitcast i8* %1 to %X*
  %3 = getelementptr inbounds %X, %X* %2, i32 0, i32 0
  store i32 %0, i32* %3
  ret %X* %2
}

define i32 @f(i32) {
entry:
  %1 = call %X* @box(i32 %0)
  %2 = call i32 @firstThing(%X* %1)
  %3 = call i32 @secondThing(%X* %1)
  %4 = call i32 @thirdThing(%X* %1)
  %5 = bitcast %X* %1 to i8*
  call void @free(i8* %5) ; that memory is freed here
  %6 = call i32 @internal_add_i32(i32 %3, i32 %4)
  %7 = call i32 @internal_add_i32(i32 %2, i32 %6)
  ret i32 %7
}
```

Boom! Memory freeing has been determined at compile time! This won't
work for everything, and control hasn't been implemented, but it's
getting there.

There are a lot more tests in the test suite (or at least there will
be).
