# Final Step: Star visibility и очистка
star_visibility blue_star true

# Убрать тег у игроков
tag @a[tag=event2_target] remove event2_target

# Сообщение в консоль (опционально)
tellraw @a[tag=event2_target] {"text":"Event2 sequence completed!","color":"green"}