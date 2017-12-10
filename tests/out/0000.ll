; ModuleID = 'memory passing'
source_filename = "memory passing"
target triple = "x86_64-unknown-linux-gnu"

%X = type { i32 }
%Y = type { %X* }

declare i8* @malloc(i64)

declare void @free(i8*)

declare i32 @internal_add_i32(i32, i32)

define i32 @add(i32, i32) {
entry:
  %2 = call i32 @internal_add_i32(i32 %0, i32 %1)
  ret i32 %2
}

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
