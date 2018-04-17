; ModuleID = 'b'
source_filename = "b"
target triple = "x86_64-unknown-linux-gnu"

declare i8* @malloc(i64)

declare void @free(i8*)

define i32 @g(i32) {
entry:
  %1 = call i32 @f(i32 %0)
  ret i32 %1
}

declare i32 @f(i32)
