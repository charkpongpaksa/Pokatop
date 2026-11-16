package com.example.pokatop.ui

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.example.pokatop.R
import com.example.pokatop.data.PokatopDbHelper
import com.example.pokatop.data.SessionRecord
import com.google.android.material.appbar.MaterialToolbar

class HistoryActivity : AppCompatActivity() {

    private lateinit var db: PokatopDbHelper
    private lateinit var adapter: SessionAdapter
    private lateinit var toolbar: MaterialToolbar
    private lateinit var emptyView: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)
        findViewById<View>(R.id.main).applySystemBarPadding()
        db = PokatopDbHelper(this)
        emptyView = findViewById(R.id.empty)
        toolbar = findViewById(R.id.toolbar)
        toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
        toolbar.inflateMenu(R.menu.menu_history)
        toolbar.setOnMenuItemClickListener {
            if (it.itemId == R.id.action_delete_all) {
                confirmDeleteAll()
                true
            } else {
                false
            }
        }
        adapter = SessionAdapter(::confirmDeleteOne)
        findViewById<RecyclerView>(R.id.sessions).adapter = adapter
    }

    override fun onResume() {
        super.onResume()
        reload()
    }

    override fun onDestroy() {
        db.close()
        super.onDestroy()
    }

    private fun reload() {
        val rows = db.getSessions()
        adapter.submit(rows)
        emptyView.visibility = if (rows.isEmpty()) View.VISIBLE else View.GONE
        toolbar.menu.findItem(R.id.action_delete_all)?.isVisible = rows.isNotEmpty()
    }

    private fun confirmDeleteOne(r: SessionRecord) {
        Dialogs.confirmDelete(
            this,
            R.string.delete_one_title,
            getString(R.string.delete_one_body),
            onCancel = { toast(R.string.delete_cancelled) }
        ) {
            db.deleteSession(r.id)
            toast(R.string.deleted)
            reload()
        }
    }

    private fun confirmDeleteAll() {
        Dialogs.confirmDelete(
            this,
            R.string.delete_all_title,
            getString(R.string.delete_all_body, adapter.itemCount),
            onCancel = { toast(R.string.delete_cancelled) }
        ) {
            db.deleteAllSessions()
            toast(R.string.deleted_all)
            reload()
        }
    }

    private fun toast(res: Int) = Toast.makeText(this, res, Toast.LENGTH_SHORT).show()
}
