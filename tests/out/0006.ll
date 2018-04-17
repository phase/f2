; ModuleID = 'recursive function'
source_filename = "recursive function"
target triple = "x86_64-unknown-linux-gnu"

declare i8* @malloc(i64)

declare void @free(i8*)

define i32 @f(i32) {
entry:
  %1 = call i32 @f(i32 %0)
  ret i32 %1
}
