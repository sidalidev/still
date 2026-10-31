package dev.sidali.still

// Verses shown on the card that opens Trust mode, one per day.
//
// Source: the Quran only, no other text, no commentary.
// Arabic: Uthmani script as served by the Tanzil project text (tanzil.net, Creative Commons
//   Attribution 3.0, text unchanged), fetched from api.alquran.cloud edition "quran-uthmani".
// English: Marmaduke Pickthall, "The Meaning of the Glorious Koran" (1930), public domain,
//   fetched from api.alquran.cloud edition "en.pickthall". Wording and punctuation kept as is.
// Where only part of a verse is kept, the ref says "(part)" and both languages stop at the same
// natural boundary. 94:5-6 joins two consecutive verses. The rub al-hizb sign before 39:53 is left out.

object Verses {
    data class Verse(val ref: String, val arabic: String, val english: String)

    val all: List<Verse> = listOf(
        Verse(
            ref = "Quran 13:28",
            arabic = "ٱلَّذِينَ ءَامَنُوا۟ وَتَطْمَئِنُّ قُلُوبُهُم بِذِكْرِ ٱللَّهِ ۗ أَلَا بِذِكْرِ ٱللَّهِ تَطْمَئِنُّ ٱلْقُلُوبُ",
            english = "Who have believed and whose hearts have rest in the remembrance of Allah. Verily in the remembrance of Allah do hearts find rest!",
        ),
        Verse(
            ref = "Quran 65:3 (part)",
            arabic = "وَمَن يَتَوَكَّلْ عَلَى ٱللَّهِ فَهُوَ حَسْبُهُۥٓ",
            english = "And whosoever putteth his trust in Allah, He will suffice him.",
        ),
        Verse(
            ref = "Quran 94:5-6",
            arabic = "فَإِنَّ مَعَ ٱلْعُسْرِ يُسْرًا إِنَّ مَعَ ٱلْعُسْرِ يُسْرًۭا",
            english = "But lo! with hardship goeth ease, Lo! with hardship goeth ease;",
        ),
        Verse(
            ref = "Quran 2:286 (part)",
            arabic = "لَا يُكَلِّفُ ٱللَّهُ نَفْسًا إِلَّا وُسْعَهَا",
            english = "Allah tasketh not a soul beyond its scope.",
        ),
        Verse(
            ref = "Quran 3:173 (part)",
            arabic = "حَسْبُنَا ٱللَّهُ وَنِعْمَ ٱلْوَكِيلُ",
            english = "Allah is Sufficient for us! Most Excellent is He in Whom we trust!",
        ),
        Verse(
            ref = "Quran 9:51",
            arabic = "قُل لَّن يُصِيبَنَآ إِلَّا مَا كَتَبَ ٱللَّهُ لَنَا هُوَ مَوْلَىٰنَا ۚ وَعَلَى ٱللَّهِ فَلْيَتَوَكَّلِ ٱلْمُؤْمِنُونَ",
            english = "Say: Naught befalleth us save that which Allah hath decreed for us. He is our Protecting Friend. In Allah let believers put their trust!",
        ),
        Verse(
            ref = "Quran 2:152",
            arabic = "فَٱذْكُرُونِىٓ أَذْكُرْكُمْ وَٱشْكُرُوا۟ لِى وَلَا تَكْفُرُونِ",
            english = "Therefore remember Me, I will remember you. Give thanks to Me, and reject not Me.",
        ),
        Verse(
            ref = "Quran 39:53 (part)",
            arabic = "قُلْ يَٰعِبَادِىَ ٱلَّذِينَ أَسْرَفُوا۟ عَلَىٰٓ أَنفُسِهِمْ لَا تَقْنَطُوا۟ مِن رَّحْمَةِ ٱللَّهِ ۚ إِنَّ ٱللَّهَ يَغْفِرُ ٱلذُّنُوبَ جَمِيعًا",
            english = "Say: O My slaves who have been prodigal to their own hurt! Despair not of the mercy of Allah, Who forgiveth all sins.",
        ),
        Verse(
            ref = "Quran 20:46",
            arabic = "قَالَ لَا تَخَافَآ ۖ إِنَّنِى مَعَكُمَآ أَسْمَعُ وَأَرَىٰ",
            english = "He said: Fear not. Lo! I am with you twain, Hearing and Seeing.",
        ),
    )

    /** Same verse all day, a new one each day, in a fixed cycle. dayOfYear is 1 to 366. */
    fun forToday(dayOfYear: Int): Verse = all[Math.floorMod(dayOfYear - 1, all.size)]
}
