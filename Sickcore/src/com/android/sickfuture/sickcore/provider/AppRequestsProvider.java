package com.android.sickfuture.sickcore.provider;

import com.android.sickfuture.sickcore.content.CommonProvider;
import com.android.sickfuture.sickcore.content.contract.CoreContracts;

public class AppRequestsProvider extends CommonProvider {

	@Override
	protected Class<?> getContractClass() {
		return CoreContracts.Request.class;
	}

}
