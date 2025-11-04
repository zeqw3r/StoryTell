# Step 7: Седьмой screenshake
execute as @a[tag=event2_target] run screenshake @s 1.1 10

# Запланировать следующий шаг через 2 секунды (40 тиков)
schedule function storytell:event2/steps/step8 40t