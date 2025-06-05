package com.example.pdf // Paket adını kendi projenle değiştir (örn: com.example.pdf)

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.Normalizer // Karakter normalizasyonu için eklendi

// R ve Course sınıflarının doğru paketten import edildiğinden emin ol.
// import com.example.pdf.R // Kendi paket adınla
// import com.example.pdf.Course // Kendi paket adınla

class CourseAdapter(
    private val context: Context,
    private val courses: MutableList<Course>,
    private val onTopicClickListener: (courseTitle: String, topicTitle: String) -> Unit,
    private val onPdfClickListener: (courseTitle: String, topicTitle: String, pdfAssetName: String) -> Unit
) : RecyclerView.Adapter<CourseAdapter.CourseViewHolder>() {

    inner class CourseViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val courseHeaderLayout: LinearLayout = itemView.findViewById(R.id.courseHeaderLayout)
        val courseTitleTextView: TextView = itemView.findViewById(R.id.textViewCourseTitle)
        val expandIconImageView: ImageView = itemView.findViewById(R.id.imageViewExpandIcon)
        val topicsContainerLayout: LinearLayout = itemView.findViewById(R.id.topicsContainerLayout)

        init {
            courseHeaderLayout.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val course = courses[position]
                    course.isExpanded = !course.isExpanded
                    notifyItemChanged(position)
                }
            }
        }
    }

    // Türkçe karakterleri İngilizce benzerlerine çeviren ve formatlayan yardımcı fonksiyon
    private fun normalizeAndFormatForAssetName(input: String): String {
        // Önce Normalizer ile aksanları ayır (örn: "ğ" -> "g" + aksan)
        var normalized = Normalizer.normalize(input, Normalizer.Form.NFD)
        // Aksan işaretlerini kaldır
        normalized = normalized.replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
        // Bilinen Türkçe karakterleri İngilizce benzerlerine çevir
        normalized = normalized
            .replace("ı", "i")
            .replace("İ", "I") // Büyük İ'yi de büyük I yapalım, lowercase sonra halleder
            .replace("ğ", "g")
            .replace("Ğ", "G")
            .replace("ü", "u")
            .replace("Ü", "U")
            .replace("ş", "s")
            .replace("Ş", "S")
            .replace("ö", "o")
            .replace("Ö", "O")
            .replace("ç", "c")
            .replace("Ç", "C")
        // Son olarak küçük harfe çevir ve boşlukları alt çizgi yap
        return normalized.lowercase().replace(" ", "_")
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CourseViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_course_with_topics, parent, false)
        return CourseViewHolder(view)
    }

    override fun onBindViewHolder(holder: CourseViewHolder, position: Int) {
        val course = courses[position]
        holder.courseTitleTextView.text = course.title

        if (course.isExpanded) {
            holder.expandIconImageView.setImageResource(R.drawable.ic_expand_less)
            holder.topicsContainerLayout.visibility = View.VISIBLE
            holder.topicsContainerLayout.removeAllViews()

            course.topics.forEach { topicTitle ->
                val topicView = LayoutInflater.from(holder.itemView.context)
                    .inflate(R.layout.item_topic, holder.topicsContainerLayout, false)
                val topicTextView: TextView = topicView.findViewById(R.id.textViewTopicTitle)
                val pdfIconImageView: ImageView = topicView.findViewById(R.id.imageViewPdfIcon)
                topicTextView.text = topicTitle

                // Güncellenmiş PDF dosya adını oluşturma mantığı
                val courseTitleForAsset = normalizeAndFormatForAssetName(course.title)
                val topicTitleForAsset = normalizeAndFormatForAssetName(topicTitle)
                val pdfAssetName = "${courseTitleForAsset}_${topicTitleForAsset}.pdf"

                // Loglama (Hata ayıklama için çok faydalı)
                val fileActuallyExists = assetExists(pdfAssetName)
                android.util.Log.d("PdfAssetCheck", "Ders: '${course.title}', Konu: '${topicTitle}' -> Aranan PDF: '$pdfAssetName', Bulundu mu?: $fileActuallyExists")

                if (fileActuallyExists) {
                    pdfIconImageView.visibility = View.VISIBLE
                    topicView.setOnClickListener {
                        onPdfClickListener(course.title, topicTitle, pdfAssetName)
                    }
                } else {
                    pdfIconImageView.visibility = View.GONE
                    topicView.setOnClickListener {
                        onTopicClickListener(course.title, topicTitle)
                    }
                }
                holder.topicsContainerLayout.addView(topicView)
            }
        } else {
            holder.expandIconImageView.setImageResource(R.drawable.ic_expand_more)
            holder.topicsContainerLayout.visibility = View.GONE
        }
    }

    override fun getItemCount(): Int = courses.size

    private fun assetExists(fileName: String): Boolean {
        return try {
            context.assets.open(fileName).use { it.close() }
            true
        } catch (e: Exception) {
            false
        }
    }
}