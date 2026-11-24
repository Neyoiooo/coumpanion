package com.coucou.android

import com.coucou.android.core.ProviderAppearance
import java.io.File
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class CoupiPixelArtTest {
    @Test fun masterAndEveryPoseUseTheSameGrid() {
        val root = File("src/main/assets/avatars")
        val source = JSONObject(File(root, "pixel-art.json").readText())
        val edge = source.getInt("size")
        val palette = source.getJSONArray("palette")
        val master = source.getJSONArray("master")
        val poses = source.getJSONArray("poses")
        val names = source.getJSONArray("names")
        assertEquals(32, edge)
        assertEquals(edge * edge, master.length())
        assertEquals(names.length(), poses.length())
        assertEquals(names.length(), (0 until names.length()).map { names.getString(it) }.distinct().size)
        for (i in 0 until poses.length()) {
            val pose = poses.getJSONArray(i)
            assertEquals(edge * edge, pose.length())
            for (p in 0 until pose.length()) assertTrue(pose.getInt(p) in 0 until palette.length())
        }
        assertEquals(0xA9ABC9, ProviderAppearance.resolve("coumpanion").accent)
    }
}
