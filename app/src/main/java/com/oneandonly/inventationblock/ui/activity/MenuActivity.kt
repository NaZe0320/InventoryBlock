package com.oneandonly.inventationblock.ui.activity

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import com.oneandonly.inventationblock.R
import com.oneandonly.inventationblock.databinding.ActivityMenuBinding
import com.oneandonly.inventationblock.ui.fragment.MenuFragment

class MenuActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMenuBinding

    private val fragmentManager = supportFragmentManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this@MenuActivity, R.layout.activity_menu)
        binding.lifecycleOwner = this@MenuActivity

        changeFragment(MenuFragment(),"Menu")
        setViewModel()
        uiSetting()
    }

    private fun setViewModel() {

    }

    private fun uiSetting() {
        toolbarSetting()
    }



    private fun toolbarSetting() {
        binding.menuToolBar.toolBarBackBtn.setOnClickListener {
            onBackPressed()
        }
        binding.menuToolBar.toolBarTitle.text = "메뉴 별 재고 설정"
    }

    override fun onBackPressed() {
        for(fragment: Fragment in supportFragmentManager.fragments) {
            when (fragment.tag) {
                "Menu" -> {
                    finish()
                }
                "MenuAdd", "MenuModify" -> {
                    changeFragment(MenuFragment(),"Menu")
                    changeToolBar("메뉴 별 재고 설정")
                }
            }
        }
    }

    fun changeFragment(fragment: Fragment, tag: String) {
        val transaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.fl_menu_menu, fragment, tag)
        transaction.commit()
    }

    fun changeToolBar(name: String) {
        binding.menuToolBar.toolBarTitle.text = name
    }
}
