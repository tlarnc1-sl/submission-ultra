package app.submissionultra.data

import androidx.room.TypeConverter

/** enum を文字列として保存するための Room コンバータ。 */
class Converters {
    @TypeConverter
    fun fromType(type: AssignmentType): String = type.name

    @TypeConverter
    fun toType(value: String): AssignmentType = AssignmentType.valueOf(value)
}
