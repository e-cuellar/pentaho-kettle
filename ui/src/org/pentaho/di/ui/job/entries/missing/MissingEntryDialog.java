/*! ******************************************************************************
 *
 * Pentaho Data Integration
 *
 * Copyright (C) 2002-2013 by Pentaho : http://www.pentaho.com
 *
 *******************************************************************************
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 ******************************************************************************/

package org.pentaho.di.ui.job.entries.missing;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.pentaho.di.core.Const;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.plugins.KettleURLClassLoader;
import org.pentaho.di.core.plugins.PluginInterface;
import org.pentaho.di.core.plugins.PluginRegistry;
import org.pentaho.di.core.plugins.PluginTypeInterface;
import org.pentaho.di.i18n.BaseMessages;
import org.pentaho.di.job.JobMeta;
import org.pentaho.di.job.entries.missing.MissingEntry;
import org.pentaho.di.job.entry.JobEntryDialogInterface;
import org.pentaho.di.job.entry.JobEntryInterface;
import org.pentaho.di.repository.Repository;
import org.pentaho.di.ui.core.PropsUI;
import org.pentaho.di.ui.core.gui.GUIResource;
import org.pentaho.di.ui.job.entry.JobEntryDialog;
import org.pentaho.di.ui.spoon.Spoon;
import org.pentaho.di.ui.trans.step.BaseStepDialog;

public class MissingEntryDialog extends JobEntryDialog implements JobEntryDialogInterface {
  private static Class<?> PKG = MissingEntryDialog.class;

  private Shell shell;
  private Shell shellParent;
  private List<MissingEntry> missingEntries;
  private int mode;
  private PropsUI props;
  private JobEntryInterface jobEntryResult;

  public static final int MISSING_JOB_ENTRIES = 1;
  public static final int MISSING_JOB_ENTRY_ID = 2;

  public MissingEntryDialog( Shell parent, List<MissingEntry> missingEntries ) {
    super( parent, null, null, null );
    this.shellParent = parent;
    this.missingEntries = missingEntries;
    this.mode = MISSING_JOB_ENTRIES;
  }

  public MissingEntryDialog( Shell parent, JobEntryInterface jobEntryInt, Repository rep, JobMeta jobMeta ) {
    super( parent, jobEntryInt, rep, jobMeta );
    this.shellParent = parent;
    this.mode = MISSING_JOB_ENTRY_ID;
  }

  private String getErrorMessage( List<MissingEntry> missingEntries, int mode ) {
    String message = "";
    if ( mode == MISSING_JOB_ENTRIES ) {
      StringBuffer entries = new StringBuffer();
      for ( MissingEntry entry : missingEntries ) {
        if ( missingEntries.indexOf( entry ) == missingEntries.size() - 1 ) {
          entries.append( "- " + entry.getName() + " - " + entry.getMissingPluginId() + "\n\n" );
        } else {
          entries.append( "- " + entry.getName() + " - " + entry.getMissingPluginId() + "\n" );
        }
      }
      message = BaseMessages.getString( PKG, "MissingEntryDialog.MissingJobEntries", entries.toString() );
    }

    if ( mode == MISSING_JOB_ENTRY_ID ) {
      message =
          BaseMessages.getString( PKG, "MissingEntryDialog.MissingJobEntryId", jobEntryInt.getName() + " - "
              + ( (MissingEntry) jobEntryInt ).getMissingPluginId() );
    }
    return message.toString();
  }

  public JobEntryInterface open() {

    this.props = PropsUI.getInstance();
    Display display = shellParent.getDisplay();
    int margin = Const.MARGIN;

    shell =
        new Shell( shellParent, SWT.DIALOG_TRIM | SWT.RESIZE | SWT.CLOSE | SWT.MAX | SWT.MIN | SWT.ICON
            | SWT.APPLICATION_MODAL );

    props.setLook( shell );
    shell.setImage( GUIResource.getInstance().getImageSpoon() );

    FormLayout formLayout = new FormLayout();
    formLayout.marginWidth = Const.FORM_MARGIN;
    formLayout.marginHeight = Const.FORM_MARGIN;

    shell.setText( BaseMessages.getString( PKG, "MissingEntryDialog.MissingPlugins" ) );
    shell.setLayout( formLayout );

    Label image = new Label( shell, SWT.NONE );
    props.setLook( image );
    Image icon = display.getSystemImage( SWT.ICON_QUESTION );
    image.setImage( icon );

    Label error = new Label( shell, SWT.NONE );
    props.setLook( error );
    error.setText( getErrorMessage( missingEntries, mode ) );

    FormData imageData = new FormData();
    imageData.left = new FormAttachment( 0, 5 );
    imageData.right = new FormAttachment( 10, 0 );
    imageData.top = new FormAttachment( 0, 10 );
    image.setLayoutData( imageData );

    FormData errorData = new FormData();
    errorData.left = new FormAttachment( image, 5 );
    errorData.right = new FormAttachment( 90, -5 );
    errorData.top = new FormAttachment( 0, 10 );
    error.setLayoutData( errorData );

    shell.setSize( 600, 300 );
    shell.setMinimumSize( 600, 300 );

    Button searchButton = new Button( shell, SWT.PUSH );
    searchButton.setText( BaseMessages.getString( PKG, "MissingEntryDialog.SearchMarketplace" ) );
    searchButton.addSelectionListener( new SelectionAdapter() {
      public void widgetSelected( SelectionEvent e ) {
        try {
          shell.dispose();
          openMarketplace( shellParent );
        } catch ( Exception ex ) {
          ex.printStackTrace();
        }
      }
    } );

    Button closeButton = new Button( shell, SWT.PUSH );
    closeButton.setText( BaseMessages.getString( PKG, "MissingEntryDialog.Close" ) );
    closeButton.addSelectionListener( new SelectionAdapter() {
      public void widgetSelected( SelectionEvent e ) {
        shell.dispose();
        jobEntryResult = null;
      }
    } );

    Button[] buttons = null;
    if ( this.mode == MISSING_JOB_ENTRIES ) {
      Button openButton = new Button( shell, SWT.PUSH );
      openButton.setText( BaseMessages.getString( PKG, "MissingEntryDialog.OpenFile" ) );
      openButton.addSelectionListener( new SelectionAdapter() {
        public void widgetSelected( SelectionEvent e ) {
          shell.dispose();
          jobEntryResult = new MissingEntry();
        }
      } );
      buttons = new Button[] { searchButton, openButton, closeButton };
    } else {
      buttons = new Button[] { searchButton, closeButton, };
    }

    BaseStepDialog.positionBottomButtons( shell, buttons, margin, null );

    shell.open();
    while ( !shell.isDisposed() ) {
      if ( !display.readAndDispatch() ) {
        display.sleep();
      }
    }
    return jobEntryResult;
  }

  public void openMarketplace( Shell shell ) throws KettleException {
    try {

      PluginInterface plugin = findMarketPlugin();
      if ( plugin == null ) {
        throw new KettleException( "No Marketplace Plugin!" );
      }

      String pluginFolderName = plugin.getPluginDirectory().getFile();
      File folder = new File( pluginFolderName );

      // Find marketplace jar
      File[] files = folder.listFiles();
      File marketplaceJar = null;
      for ( File f : files ) {
        if ( f.getName().startsWith( "market" ) && f.getName().endsWith( ".jar" ) ) {
          marketplaceJar = f;
          break;
        }
      }

      // Load marketplace plugin and execute it
      KettleURLClassLoader classloader = null;
      try {
        classloader =
            new KettleURLClassLoader( new URL[] { marketplaceJar.toURI().toURL() }, Spoon.getInstance().getClass()
                .getClassLoader() );
        Class<?> clazz = classloader.loadClass( "org.pentaho.di.ui.spoon.dialog.MarketplaceDialog" );
        Constructor<?> constructor = clazz.getConstructor( Shell.class );
        Object obj = constructor.newInstance( shell );
        Method method = clazz.getMethod( "open" );
        method.invoke( obj );
      } catch ( Throwable t ) {
        t.printStackTrace();
      } finally {
        if ( classloader != null ) {
          classloader.closeClassLoader();
        }
      }
    } catch ( Throwable t ) {
      t.printStackTrace();
      throw new KettleException( t );
    }
  }

  private PluginInterface findMarketPlugin() {
    PluginInterface plugin = null;
    List<Class<? extends PluginTypeInterface>> pluginTypes = PluginRegistry.getInstance().getPluginTypes();
    for ( Class<? extends PluginTypeInterface> pluginType : pluginTypes ) {
      plugin = PluginRegistry.getInstance().findPluginWithId( pluginType, "market" );
      if ( plugin != null ) {
        break;
      }
    }
    return plugin;
  }
}
