package top.niunaijun.blackboxa.view.list

import androidx.lifecycle.MutableLiveData
import org.junit.Assert.assertEquals
import org.junit.Test
import top.niunaijun.blackboxa.bean.InstalledAppBean
import top.niunaijun.blackboxa.data.InstalledAppListSource

class ListViewModelTest {

    @Test
    fun chooserRefreshesHostInventoryBeforeBuildingUserList() {
        val calls = mutableListOf<String>()

        val source = object : InstalledAppListSource {
            override fun previewInstallList() {
                calls += "preview"
            }

            override fun getInstalledAppList(
                userID: Int,
                loadingLiveData: MutableLiveData<Boolean>,
                appsLiveData: MutableLiveData<List<InstalledAppBean>>
            ) {
                calls += "build:$userID"
            }
        }

        val viewModel = ListViewModel(source)
        viewModel.loadChooserInventory(2)

        assertEquals(listOf("preview", "build:2"), calls)
    }
}
