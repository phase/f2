; ModuleID = 'freeing passed memory'
source_filename = "freeing passed memory"
target triple = "x86_64-unknown-linux-gnu"

%X = type { i32 }

declare i8* @malloc(i64)

declare void @free(i8*)

declare i32 @internal_add_i32(i32, i32)

define %X* @box(i32) {
entry:
  %1 = call i8* @malloc(i64 4)
  %2 = bitcast i8* %1 to %X*
  %3 = getelementptr inbounds %X, %X* %2, i32 0, i32 0
  store i32 %0, i32* %3
  ret %X* %2
}

define i32 @firstThing(%X*) {
entry:
  %1 = getelementptr inbounds %X, %X* %0, i32 0, i32 0
  %2 = load i32, i32* %1
  ret i32 %2
}

define i32 @secondThing(%X*) {
entry:
  %1 = getelementptr inbounds %X, %X* %0, i32 0, i32 0
  %2 = load i32, i32* %1
  ret i32 %2
}

define i32 @thirdThing(%X*) {
entry:
  %1 = getelementptr inbounds %X, %X* %0, i32 0, i32 0
  %2 = load i32, i32* %1
  ret i32 %2
}

define i32 @f(i32) {
entry:
  %1 = call %X* @box(i32 %0)
  %2 = call i32 @firstThing(%X* %1)
  %3 = call i32 @secondThing(%X* %1)
  %4 = call i32 @thirdThing(%X* %1)
  %5 = bitcast %X* %1 to i8*
  call void @free(i8* %5)
  %6 = call i32 @internal_add_i32(i32 %3, i32 %4)
  %7 = call i32 @internal_add_i32(i32 %2, i32 %6)
  ret i32 %7
}
