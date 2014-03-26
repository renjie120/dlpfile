package org.vudroid.pdfdroid;

import org.vudroid.pdfdroid.codec.PdfContext;

import com.poqop.document.BaseViewerActivity;
import com.poqop.document.DecodeService;
import com.poqop.document.DecodeServiceBase;

public class PdfViewerActivity extends BaseViewerActivity
{
    @Override
    protected DecodeService createDecodeService()
    {
        return new DecodeServiceBase(new PdfContext());
    }
}
