package com.oneandonly.inventationblock.ui.activity

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.animation.AnimationUtils
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.core.view.GravityCompat
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import androidx.drawerlayout.widget.DrawerLayout.LOCK_MODE_LOCKED_CLOSED
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.oneandonly.inventationblock.Constants.tokens
import com.oneandonly.inventationblock.R
import com.oneandonly.inventationblock.afterUpdate
import com.oneandonly.inventationblock.databinding.ActivityMainBinding
import com.oneandonly.inventationblock.databinding.NavHeaderMainBinding
import com.oneandonly.inventationblock.datasource.model.data.State
import com.oneandonly.inventationblock.datasource.model.data.Stock
import com.oneandonly.inventationblock.datasource.model.repository.StockRepository
import com.oneandonly.inventationblock.datasource.model.repository.UserRepository
import com.oneandonly.inventationblock.makeToast
import com.oneandonly.inventationblock.ui.adapter.OnClick
import com.oneandonly.inventationblock.ui.adapter.StockAdapter
import com.oneandonly.inventationblock.viewmodel.AutoLoginViewModel
import com.oneandonly.inventationblock.viewmodel.StockViewModel
import com.oneandonly.inventationblock.viewmodel.TokenViewModel
import com.oneandonly.inventationblock.viewmodel.UserViewModel
import com.oneandonly.inventationblock.viewmodel.factory.StockFactory
import com.oneandonly.inventationblock.viewmodel.factory.UserFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity(), OnClick {

    private lateinit var binding: ActivityMainBinding

    private lateinit var userViewModel: UserViewModel
    private lateinit var stockViewModel: StockViewModel

    private lateinit var stockAdapter: StockAdapter

    private var searchState = false
    // true: 검색 중/ false: 검색 중 아닐 때

    private var clickTime: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = DataBindingUtil.setContentView(this@MainActivity,R.layout.activity_main)
        binding.lifecycleOwner = this@MainActivity

        setViewModel()

        Log.d("Token Main",tokens.toString())

        val navBind: NavHeaderMainBinding = NavHeaderMainBinding.bind(binding.mainNavView.getHeaderView(0))
        navBind.user = userViewModel //실시간 변경이 안됨 //TODO(수정 필요)
        binding.user = userViewModel
        binding.mainToolBar.user = userViewModel

        //UI
        uiSetting()

        //Observer
        stockListObserver()
        errorObserver()

    }

    override fun onStart() {
        super.onStart()
        userViewModel.getInformation()
    }

    override fun onResume() {
        super.onResume()
        resetList()
    }

    override fun onBackPressed() {

        if (binding.mainSearchEdit.hasFocus()) {
            binding.mainSearchEdit.clearFocus()
        } else {
            //두번 클릭 종료
            val current = System.currentTimeMillis()
            if (current - clickTime >= 2500) {
                clickTime = current
                makeToast("한번 더 클릭 시 종료됩니다.")
            } else {
                super.onBackPressed()
            }
        }
    }

    private fun observeViewModel() {
        userViewModel.user.observe(this@MainActivity, Observer { user ->
            user?.let {

            }
        })
    }

    private fun setViewModel() {
        val repository = UserRepository()
        val stockRepo = StockRepository()

        val viewModelFactory = UserFactory(repository)
        userViewModel = ViewModelProvider(this@MainActivity,viewModelFactory)[UserViewModel::class.java]

        val stockViewModelFactory = StockFactory(stockRepo)
        stockViewModel = ViewModelProvider(this@MainActivity,stockViewModelFactory)[StockViewModel::class.java]

    }

    private fun onClickLogout() {
        val autoLoginViewModel = AutoLoginViewModel()
        val tokenViewModel = TokenViewModel()

        tokenViewModel.updateToken("null")

        autoLoginViewModel.updateAutoLogin(false)
        moveToLogin()
    }

    private fun moveToLogin() {
        Log.d("Main_Activity","moveToLogin")
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun uiSetting() {
        drawerSetting()
        toolBarSetting()
        stockListSetting(stockViewModel)
        searchEditSetting()
        fabSetting()
    }

    private fun drawerSetting() {
        binding.mainDrawer.setDrawerLockMode(LOCK_MODE_LOCKED_CLOSED)  //열 때는 드로우 잠김, 닫을 때는 드로우 가능
        binding.mainNavView.setNavigationItemSelectedListener {
            when (it.itemId) {
                R.id.menu_myPage -> {
                    afterUpdate()
                }
                R.id.menu_logout -> {
                    onClickLogout()
                }
                R.id.menu_stockSetting -> {
                    afterUpdate()
                }
                R.id.menu_stockReport -> {
                    afterUpdate()
                }
            }
            binding.mainDrawer.closeDrawers()
            return@setNavigationItemSelectedListener false
        }
    }

    private fun toolBarSetting() {
        binding.mainToolBar.toolBarDrawerBtn.setOnClickListener {
            binding.mainDrawer.openDrawer(GravityCompat.START)
        }

        binding.mainToolBar.toolBarAlarmBtn.setOnClickListener {
            afterUpdate()
            //TODO(아직 결정된 바 없음)
        }

        binding.mainToolBar.toolBarMyPageBtn.setOnClickListener {
            afterUpdate()
            //TODO(마이페이지로 연결)
        }

        binding.mainToolBar.toolBarTitle.setOnClickListener {
            //TODO(리사이클러뷰 리스트 초기화)
        }
    }

    private fun stockListSetting(stockViewModel: StockViewModel) {
        binding.stockList.layoutManager = LinearLayoutManager(this)
        stockAdapter = StockAdapter(stockViewModel.stockList, stockViewModel, this)
        binding.stockList.adapter = stockAdapter

        CoroutineScope(Dispatchers.Main).launch {
            stockViewModel.getStockList(0) //TODO(정렬 기능 설정 안됨, 스피너 쪽에서 조정이 필요함함)
        }
    }

    private fun searchEditSetting() {
        binding.mainSearchEdit.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                changeStateSearch(true)
            }
        }

        binding.mainSearchEdit.setOnEditorActionListener { _, i, _ ->
            if ( i == EditorInfo.IME_ACTION_SEARCH) {
                search()
                return@setOnEditorActionListener true
            } else {
                return@setOnEditorActionListener false
            }
        }

        binding.mainIcon.setOnClickListener {
            changeStateSearch(false)
        }

        binding.mainSearchBtn.setOnClickListener {
            search()
        }

        binding.mainToolBar.toolBarTitle.setOnClickListener {
            if (searchState) {
                changeStateSearch(false)
                stockViewModel.getStockList(0) //TODO
                searchState = false
                binding.mainSearchEdit.text.clear()
            } else {
                changeStateSearch(false)
            }

        }
    }

    private fun fabSetting() {
        var clickedFab = false

        val rotateOpen = AnimationUtils.loadAnimation(applicationContext,R.anim.rotate_open_anim)
        val rotateClose = AnimationUtils.loadAnimation(applicationContext,R.anim.rotate_close_anim)
        val toBottom = AnimationUtils.loadAnimation(applicationContext,R.anim.to_bottom_anim)
        val fromBottom = AnimationUtils.loadAnimation(applicationContext,R.anim.from_bottom_anim)

        binding.mainFab.setOnClickListener {
            clickedFab = !clickedFab
            binding.let {
                it.fabLayout1.isVisible = clickedFab
                it.fabLayout2.isVisible = clickedFab
                it.fabLayout3.isVisible = clickedFab

                if(clickedFab) {
                    it.mainFab.startAnimation(rotateOpen)
                    it.fabLayout1.startAnimation(fromBottom)
                    it.fabLayout2.startAnimation(fromBottom)
                    it.fabLayout3.startAnimation(fromBottom)
                } else {
                    it.mainFab.startAnimation(rotateClose)
                    it.fabLayout1.startAnimation(toBottom)
                    it.fabLayout2.startAnimation(toBottom)
                    it.fabLayout3.startAnimation(toBottom)
                }

                it.fab1.isClickable = clickedFab
                it.fab2.isClickable = clickedFab
                it.fab3.isClickable = clickedFab

                it.sticker.visibility = if(clickedFab) View.VISIBLE else View.GONE
            }
        }

        binding.fab1.setOnClickListener {
            afterUpdate()
        }

        binding.fab2.setOnClickListener {
            afterUpdate()
        }

        binding.fab3.setOnClickListener {
            afterUpdate()
        }
    }

    private fun stockListObserver() {
        val stockObserver: Observer<ArrayList<Stock>> = Observer {
                stockAdapter = StockAdapter(stockViewModel.stockList, stockViewModel, this)
                binding.stockList.adapter = stockAdapter
                Log.d("Main_Activity","4")
        }
        stockViewModel.stockList.observe(this,stockObserver)
    }

    private fun errorObserver() {
        val errorObserver: Observer<String> = Observer {
            makeToast(stockViewModel.errorList.value.toString())
        }
        stockViewModel.errorList.observe(this,errorObserver)
    }

    private fun changeStateSearch(state: Boolean) { //검색 모드 변경 시 체크
        Log.d("State_Change","변경 $state")

        binding.let {
            it.mainListAlign.visibility = if (state) View.GONE else View.VISIBLE
            it.mainFab.isInvisible = state
            it.stockList.isInvisible = state
            it.mainIcon.isClickable = state
            it.mainIcon.setImageResource(if (state) R.drawable.ic_back else R.drawable.ic_logo)
            if (!state) {
                it.mainSearchEdit.clearFocus()
                hideKeyBoard()
            }
        }
    }

    private fun search() { //검색
        searchState = true
        stockViewModel.getSearchList(binding.mainSearchEdit.text.toString())
        Log.d("Search","!")
        changeStateSearch(false) //검색하면 검색 중 종료
    }

    private fun hideKeyBoard() {
        try {
            val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(binding.mainSearchEdit.windowToken, 0)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    } //키보드 숨기기

    override fun onClick() {
        stockViewModel.ing.value = State.Loading
        stockViewModel.ing.observe(this) {
            when (it) {
                State.Success -> {
                    resetList()
                    Log.d("ToggleClick","Sucess")
                }
                State.Fail -> {
                    Log.d("ToggleClick",".Fail")
                }
                State.Loading -> {
                    Log.d("ToggleClick","Loading")
                }
            }
        }
    }

    private fun resetList() {
        if (searchState) {
            stockViewModel.getSearchList(binding.mainSearchEdit.text.toString())
        } else {
            stockViewModel.getStockList(0) //TODO(현재 스피너 값 가져오기)
        }
    }

}