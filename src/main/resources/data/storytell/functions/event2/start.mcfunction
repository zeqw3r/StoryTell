# Event2 Start Function
# Запускает всю последовательность события

# 1. Немедленно проиграть звук
execute as @a[tag=event2_target] run playsound storytell:event2 master @s ~ ~ ~ 20.0

# 2. Запланировать первый screenshake через 1 секунду (20 тиков)
schedule function storytell:event2/steps/step1 20t