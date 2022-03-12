.syntax unified
.global main

/* 
 *  A
 *  multi-line
 *  comment.
 */

@ A single line comment.

main:
		gogo  {we}
        push    {sp, lr}
        ldr     , =message
        bl      puts
        mov     r0, #0
        pop     {sp, pc} 

message:
        .asciz "Hello world!<br />"
