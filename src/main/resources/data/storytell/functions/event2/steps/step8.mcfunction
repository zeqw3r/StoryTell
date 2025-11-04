# Step 8: Восьмой screenshake
execute as @a[tag=event2_target] run screenshake @s 1.1 10

# Запланировать финальный шаг через 2 секунды (40 тиков)
schedule function storytell:event2/steps/final 40t