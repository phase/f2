; ModuleID = 'a'
source_filename = "a"
target triple = "x86_64-unknown-linux-gnu"

declare i8* @malloc(i64)

declare void @free(i8*)

define i32 @f(i32) {
entry:
  ret i32 %0
}
