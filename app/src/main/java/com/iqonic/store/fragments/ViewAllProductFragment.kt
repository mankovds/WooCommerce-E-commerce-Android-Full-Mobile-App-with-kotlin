package com.iqonic.store.fragments

import android.graphics.Color
import android.os.Bundle
import android.view.*
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.iqonic.store.AppBaseActivity
import com.iqonic.store.R
import com.iqonic.store.activity.*
import com.iqonic.store.adapter.BaseAdapter
import com.iqonic.store.models.Category
import com.iqonic.store.models.RequestModel
import com.iqonic.store.models.SearchRequest
import com.iqonic.store.models.StoreProductModel
import com.iqonic.store.utils.Constants
import com.iqonic.store.utils.Constants.KeyIntent.SPECIAL_PRODUCT_KEY
import com.iqonic.store.utils.Constants.TotalItem.TOTAL_ITEM_PER_PAGE
import com.iqonic.store.utils.Constants.TotalItem.TOTAL_SUB_CATEGORY_PER_PAGE
import com.iqonic.store.utils.Constants.viewAllCode.BESTSELLING
import com.iqonic.store.utils.Constants.viewAllCode.CATEGORY
import com.iqonic.store.utils.Constants.viewAllCode.FEATURED
import com.iqonic.store.utils.Constants.viewAllCode.NEWEST
import com.iqonic.store.utils.Constants.viewAllCode.SALE
import com.iqonic.store.utils.Constants.viewAllCode.SPECIAL_PRODUCT
import com.iqonic.store.utils.extensions.*
import kotlinx.android.synthetic.main.fragment_newest_product.*
import kotlinx.android.synthetic.main.item_subcategory.view.*
import kotlinx.android.synthetic.main.item_viewproductgrid.view.ivProduct
import kotlinx.android.synthetic.main.item_viewproductgrid.view.tvAdd
import kotlinx.android.synthetic.main.item_viewproductgrid.view.tvDiscountPrice
import kotlinx.android.synthetic.main.item_viewproductgrid.view.tvOriginalPrice
import kotlinx.android.synthetic.main.item_viewproductgrid.view.tvProductName
import kotlinx.android.synthetic.main.item_viewproductgrid.view.tvProductWeight
import kotlinx.android.synthetic.main.menu_cart.view.*


class ViewAllProductFragment : BaseFragment() {

    private var showPagination: Boolean? = true
    private var mIsLoading = false
    private var countLoadMore = 1
    private var subCategoryCountLoadMore = 1
    private var mCategoryId: Int = 0
    private lateinit var mProductAttributeResponseMsg: String
    private var menuCart: View? = null
    private var mId: Int = 0
    private var mColorArray = intArrayOf(R.color.cat_1)
    private var searchRequest = SearchRequest()
    private var specialProduct = ""
    private var totalPages = 0
    private val data: MutableMap<String, Int> =
        HashMap()
    private val subCategoryData: MutableMap<String, Int> =
        HashMap()
    private var isLastPage: Boolean? = false
    private var mIsLastPage: Boolean? = false

    companion object {
        fun getNewInstance(
            id: Int,
            mCategoryId: Int,
            showPagination: Boolean = true,
            specialProduct: String = ""
        ): ViewAllProductFragment {

            val fragment = ViewAllProductFragment()
            val bundle = Bundle()
            bundle.putSerializable(Constants.KeyIntent.VIEWALLID, id)
            bundle.putSerializable(Constants.KeyIntent.KEYID, mCategoryId)
            bundle.putSerializable(Constants.KeyIntent.SHOW_PAGINATION, showPagination)
            if (specialProduct.isNotEmpty()) {
                bundle.putSerializable(SPECIAL_PRODUCT_KEY, specialProduct)
            }

            fragment.arguments = bundle
            return fragment
        }
    }

    private val mSubCategoryAdapter =
        BaseAdapter<Category>(R.layout.item_subcategory, onBind = { view, model, position ->
            view.tvSubCategory.text = model.name
            if (model.image != null) {
                if (model.image.src.isNotEmpty()) {
                    view.ivProducts.loadImageFromUrl(model.image.src)
                    view.ivProducts.visibility = View.VISIBLE
                }
            } else {
                view.ivProducts.visibility = View.GONE
            }
            view.llMain.setStrokedBackground(
                (activity as AppBaseActivity).color(R.color.transparent),
                (activity as AppBaseActivity).color(mColorArray[position % mColorArray.size])
            )
            view.tvSubCategory.setTextColor((activity as AppBaseActivity).color(mColorArray[position % mColorArray.size]))
            view.onClick {
                (activity as AppBaseActivity).launchActivity<SubCategoryActivity> {
                    putExtra(Constants.KeyIntent.TITLE, model.name)
                    putExtra(Constants.KeyIntent.KEYID, model.id)
                }
            }
            view.tvSubCategory.changeTextPrimaryColor()
            view.llMain.setStrokedBackground(
                Color.parseColor(getTextPrimaryColor()),
                Color.parseColor(getTextPrimaryColor()),
                0.4f
            )
        })

    private val mProductAdapter =
        BaseAdapter<StoreProductModel>(R.layout.item_viewproductgrid, onBind = { view, model, _ ->

            if (model.images!!.isNotEmpty()) {
                if (model.images!![0].src!!.isNotEmpty()) {
                    view.ivProduct.loadImageFromUrl(model.images!![0].src!!)
                }
            } else {
                view.ivProduct.loadImageFromDrawable(R.drawable.app_logo)
            }

            val mName = model.name!!.split(",")

            view.tvProductName.text = mName[0]
            view.tvProductWeight.changePrimaryColor()
            view.tvProductName.changeTextPrimaryColor()
            view.tvOriginalPrice.changeTextSecondaryColor()
            view.tvDiscountPrice.changeTextPrimaryColor()
            view.tvAdd.background.setTint(Color.parseColor(getAccentColor()))

            if (!model.onSale) {
                view.tvDiscountPrice.text = model.price!!.currencyFormat()
                view.tvOriginalPrice.visibility = View.VISIBLE
                view.tvOriginalPrice.text = ""
            } else {
                if (model.salePrice!!.isNotEmpty()) {
                    view.tvDiscountPrice.text = model.salePrice!!.currencyFormat()
                    view.tvOriginalPrice.applyStrike()
                    view.tvOriginalPrice.text = model.regularPrice!!.currencyFormat()
                    view.tvOriginalPrice.visibility = View.VISIBLE
                } else {
                    view.tvOriginalPrice.visibility = View.VISIBLE
                    if (model.regularPrice!!.isEmpty()) {
                        view.tvOriginalPrice.text = ""
                        view.tvDiscountPrice.text = model.price!!.currencyFormat()
                    } else {
                        view.tvOriginalPrice.text = ""
                        view.tvDiscountPrice.text = model.regularPrice!!.currencyFormat()
                    }
                }
            }
            if (model.attributes!!.isNotEmpty()) {
                view.tvProductWeight.text = model.attributes!![0].options!![0]
            }
            if (model.in_stock) {
                view.tvAdd.show()
            } else {
                view.tvAdd.hide()
            }
            if (!model.purchasable) {
                view.tvAdd.hide()
            } else {
                view.tvAdd.show()
            }
            view.onClick {
                if(getProductDetailConstant()==0){
                    launchActivity<ProductDetailActivity1> {
                        putExtra(Constants.KeyIntent.PRODUCT_ID, model.id)
                        putExtra(Constants.KeyIntent.DATA, model)
                    }
                }
                else{
                    launchActivity<ProductDetailActivity2> {
                        putExtra(Constants.KeyIntent.PRODUCT_ID, model.id)
                        putExtra(Constants.KeyIntent.DATA, model)
                    }
                }
            }
            view.tvAdd.onClick {
                addCart(model)
            }
        })

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_newest_product, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setHasOptionsMenu(true)

        mId = arguments?.getInt(Constants.KeyIntent.VIEWALLID)!!
        if (mId == SPECIAL_PRODUCT) {
            specialProduct = arguments?.getString(SPECIAL_PRODUCT_KEY)!!
        }
        mCategoryId = arguments?.getInt(Constants.KeyIntent.KEYID)!!
        showPagination = arguments?.getBoolean(Constants.KeyIntent.SHOW_PAGINATION)
        mProductAttributeResponseMsg = getString(R.string.lbl_please_wait)

        val linearLayoutManager = GridLayoutManager(activity, 2)
        rvNewestProduct.layoutManager = linearLayoutManager
        fLMain.changeBackgroundColor()
        if (mId == CATEGORY) {
            data["page"] = countLoadMore
            data["per_page"] = TOTAL_ITEM_PER_PAGE
            data["category"] = mCategoryId
            loadCategory(data)

            subCategoryData["per_page"] = TOTAL_SUB_CATEGORY_PER_PAGE
            subCategoryData["parent"] = mCategoryId
            loadSubCategory(subCategoryData)
        } else {
            when (mId) {
                FEATURED -> searchRequest.featured = "product_visibility"
                NEWEST -> searchRequest.newest = "newest"
                SALE -> searchRequest.on_sale = "_sale_price"
                BESTSELLING -> searchRequest.Optional_selling = "total_sales"
                SPECIAL_PRODUCT -> searchRequest.special_product = specialProduct
            }
            searchRequest.product_per_page=TOTAL_ITEM_PER_PAGE
            searchRequest.page = countLoadMore
            loadData()
        }

        rvCategory.apply {
            setHorizontalLayout(false)
            setHasFixedSize(true)
            rvCategory.adapter = mSubCategoryAdapter
            rvCategory.rvItemAnimation()
        }

        rvNewestProduct.apply {
            rvNewestProduct.rvItemAnimation()
            rvNewestProduct.adapter = mProductAdapter

            if (showPagination!!) {
                addOnScrollListener(object : RecyclerView.OnScrollListener() {
                    override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                        super.onScrollStateChanged(recyclerView, newState)
                        val countItem = recyclerView.layoutManager?.itemCount

                        var lastVisiblePosition = 0
                        if (recyclerView.layoutManager is LinearLayoutManager) {
                            lastVisiblePosition =
                                (recyclerView.layoutManager as LinearLayoutManager).findLastCompletelyVisibleItemPosition()
                        } else if (recyclerView.layoutManager is GridLayoutManager) {
                            lastVisiblePosition =
                                (recyclerView.layoutManager as GridLayoutManager).findLastCompletelyVisibleItemPosition()
                        }
                        if (mId == CATEGORY) {
                            if (isLastPage == false) {
                                if (lastVisiblePosition != 0 && !mIsLoading && countItem?.minus(1) == lastVisiblePosition) {
                                    mIsLoading = true
                                    countLoadMore = countLoadMore.plus(1)
                                    data["page"] = countLoadMore
                                    data["per_page"] = TOTAL_ITEM_PER_PAGE
                                    data["category"] = mCategoryId
                                    loadCategory(data)
                                }
                            }
                        } else {
                            if (lastVisiblePosition != 0 && !mIsLoading && countItem?.minus(1) == lastVisiblePosition && totalPages > countLoadMore) {
                                mIsLoading = true
                                countLoadMore = countLoadMore.plus(1)
                                searchRequest.page = countLoadMore
                                searchRequest.product_per_page=TOTAL_ITEM_PER_PAGE
                                loadData()
                            }
                        }
                    }
                })
            }
        }

    }

    private fun addCart(model: StoreProductModel) {
        if (isLoggedIn()) {
            val requestModel = RequestModel()
            if (model.type == "variable") {
                requestModel.pro_id = model.variations!![0]
            } else {
                requestModel.pro_id = model.id
            }
            requestModel.quantity = 1
            (activity as AppBaseActivity).addItemToCart(requestModel, onApiSuccess = {
                activity!!.fetchAndStoreCartData()
            })
        } else (activity as AppBaseActivity).launchActivity<SignInUpActivity> { }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        menu.clear()
        inflater.inflate(R.menu.menu_view_all, menu)
        val menuWishItem = menu.findItem(R.id.action_cart)
        menuWishItem.isVisible = true
        menuCart = menuWishItem.actionView
        menuWishItem.actionView.onClick {
            if (isLoggedIn()) {
                launchActivity<MyCartActivity>()
            } else {
                launchActivity<SignInUpActivity>()
            }
        }
        setCartCount()
        super.onCreateOptionsMenu(menu, inflater)
    }

    fun setCartCount() {
        val count = getCartCount()
        if (menuCart != null) {
            menuCart?.ivCart?.changeBackgroundImageTint(getTextTitleColor())
            menuCart?.tvNotificationCount?.changeTint(getTextTitleColor())
            menuCart!!.tvNotificationCount.text = count
            menuCart!!.tvNotificationCount.changeAccentColor()
            if (count.checkIsEmpty() || count == "0") {
                menuCart!!.tvNotificationCount.hide()
            } else {
                menuCart!!.tvNotificationCount.show()
            }
        }

    }

    private fun loadCategory(data: MutableMap<String, Int>) {
        if (isNetworkAvailable()) {
            (activity!! as AppBaseActivity).showProgress(true)
            getRestApiImpl().listAllCategoryProduct(data, onApiSuccess = {
                if (activity == null) return@listAllCategoryProduct
                (activity!! as AppBaseActivity).showProgress(false)
                if (countLoadMore == 1) {
                    mProductAdapter.clearItems()
                }
                if (it.isEmpty()) {
                    isLastPage = true
                }
                mIsLoading = false
                mProductAdapter.addMoreItems(it)
                if (mProductAdapter.itemCount == 0) {
                    rvNewestProduct.hide()
                } else {
                    rvNewestProduct.show()
                }

            }, onApiError = {
                if (activity == null) return@listAllCategoryProduct
                (activity!! as AppBaseActivity).showProgress(false)
                snackBar(it)
            })

        } else {
            (activity!! as AppBaseActivity).showProgress(false)
            (activity as AppBaseActivity).noInternetSnackBar()
        }
    }

    private fun loadSubCategory(data: MutableMap<String, Int>) {
        if (isNetworkAvailable()) {
            (activity!! as AppBaseActivity).showProgress(true)
            getRestApiImpl().listAllCategory(data, onApiSuccess = {
                if (activity == null) return@listAllCategory
                (activity!! as AppBaseActivity).showProgress(false)
                if (subCategoryCountLoadMore == 1) {
                    mSubCategoryAdapter.clearItems()
                }
                if (it.isEmpty()) {
                    mIsLastPage = true
                }
                mIsLoading = false
                mSubCategoryAdapter.addMoreItems(it)
                if (mSubCategoryAdapter.itemCount == 0) {
                    rvCategory.hide()
                } else {
                    rvCategory.show()
                }
            }, onApiError = {
                (activity!! as AppBaseActivity).showProgress(false)
                snackBar(it)
            })

        }
    }

    private fun loadData() {
        if (isNetworkAvailable()) {
            (activity!! as AppBaseActivity).showProgress(true)
            getRestApiImpl().listSaleProducts(searchRequest, onApiSuccess = {
                if (activity == null) return@listSaleProducts
                (activity!! as AppBaseActivity).showProgress(false)
                if (countLoadMore == 1) {
                    mProductAdapter.clearItems()
                }
                mIsLoading = false
                totalPages = it.numOfPages
                mProductAdapter.addMoreItems(it.data!!)
            }, onApiError = {
                (activity!! as AppBaseActivity).showProgress(false)
                snackBar(it)
            })

        }
    }


}
