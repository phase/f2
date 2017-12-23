; ModuleID = 'free returned allocation'
source_filename = "free returned allocation"
target triple = "x86_64-unknown-linux-gnu"

%X = type { i32 }

declare i8* @malloc(i64)

declare void @free(i8*)

define %X* @box(i32) {
entry:
  %1 = call i8* @malloc(i64 4)
  %2 = bitcast i8* %1 to %X*
  %3 = getelementptr inbounds %X, %X* %2, i32 0, i32 0
  store i32 %0, i32* %3
  ret %X* %2
}

define i32 @f(i32) {
entry:
  %1 = call %X* @box(i32 %0)
  %2 = bitcast %X* %1 to i8*
  call void @free(i8* %2)
  ret i32 %0
}
