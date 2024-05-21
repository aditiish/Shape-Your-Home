import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class User(
    val uid: String,
    val name: String,
    var lon: Double,
    var lat: Double,
    val profileImageUrl: String?,
    var contacts: ArrayList<String>? = arrayListOf(uid),
    var role: String
) : Parcelable {
    constructor() : this("", "", 0.0, 0.0, "", ArrayList<String>(),"")
}
