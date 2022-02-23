/*
 * Copyright (c) 2019 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.labkey.panoramapublic;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.labkey.api.collections.CaseInsensitiveHashSet;
import org.labkey.api.data.Container;
import org.labkey.api.data.ContainerFilter;
import org.labkey.api.data.ContainerForeignKey;
import org.labkey.api.data.DbSchema;
import org.labkey.api.data.DbSchemaType;
import org.labkey.api.data.EnumTableInfo;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.TableInfo;
import org.labkey.api.data.dialect.SqlDialect;
import org.labkey.api.module.Module;
import org.labkey.api.query.DefaultSchema;
import org.labkey.api.query.FieldKey;
import org.labkey.api.query.FilteredTable;
import org.labkey.api.query.QueryForeignKey;
import org.labkey.api.query.QuerySchema;
import org.labkey.api.query.QuerySettings;
import org.labkey.api.query.QueryView;
import org.labkey.api.query.UserSchema;
import org.labkey.api.security.User;
import org.labkey.api.view.ViewContext;
import org.labkey.panoramapublic.model.speclib.SpecLibDependencyType;
import org.labkey.panoramapublic.model.speclib.SpecLibSourceType;
import org.labkey.panoramapublic.model.validation.Modification.ModType;
import org.labkey.panoramapublic.model.validation.PxStatus;
import org.labkey.panoramapublic.model.validation.SpecLibSourceFile.LibrarySourceFileType;
import org.labkey.panoramapublic.query.DataValidationTableInfo;
import org.labkey.panoramapublic.query.ExperimentAnnotationsTableInfo;
import org.labkey.panoramapublic.query.JournalExperimentTableInfo;
import org.labkey.panoramapublic.query.PanoramaPublicTable;
import org.labkey.panoramapublic.query.SubmissionTableInfo;
import org.labkey.panoramapublic.query.speclib.SpecLibInfoTableInfo;
import org.springframework.validation.BindException;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class PanoramaPublicSchema extends UserSchema
{
    public static final String SCHEMA_NAME = "panoramapublic";
    public static final String SCHEMA_DESCR = "Contains data for Panorama Public";

    public static final String TABLE_JOURNAL = "Journal";
    public static final String TABLE_JOURNAL_EXPERIMENT = "JournalExperiment";
    public static final String TABLE_SUBMISSION = "Submission";
    public static final String TABLE_EXPERIMENT_ANNOTATIONS = "ExperimentAnnotations";
    public static final String TABLE_PX_XML = "PxXml";
    public static final String TABLE_DATA_VALIDATION = "DataValidation";
    public static final String TABLE_SKYLINE_DOC_VALIDATION = "SkylineDocValidation";
    public static final String TABLE_SKYLINE_DOC_SAMPLE_FILE = "SkylineDocSampleFile";
    public static final String TABLE_MODIFICATION_VALIDATION = "ModificationValidation";
    public static final String TABLE_SKYLINE_DOC_MODIFICATION = "SkylineDocModification";
    public static final String TABLE_SPEC_LIB_VALIDATION = "SpecLibValidation";
    public static final String TABLE_SKYLINE_DOC_SPEC_LIB = "SkylineDocSpecLib";
    public static final String TABLE_SPEC_LIB_SOURCE_FILE = "SpecLibSourceFile";
    public static final String TABLE_LIBRARY_SOURCE_FILE_TYPE = "LibrarySourceFileType";
    public static final String TABLE_PX_STATUS = "PxStatus";
    public static final String TABLE_MOD_TYPE = "ModType";
    public static final String TABLE_SPEC_LIB_INFO = "SpecLibInfo";

    public static final String TABLE_LIB_DEPENDENCY_TYPE = "SpecLibDependencyType";
    public static final String TABLE_LIB_SOURCE_TYPE = "SpecLibSourceType";

    public PanoramaPublicSchema(User user, Container container)
    {
        super(SCHEMA_NAME, SCHEMA_DESCR, user, container, getSchema());
    }

    static public void register(Module module)
    {
        DefaultSchema.registerProvider(SCHEMA_NAME, new DefaultSchema.SchemaProvider(module)
        {
            @Override
            public QuerySchema createSchema(DefaultSchema schema, Module module)
            {
                return new PanoramaPublicSchema(schema.getUser(), schema.getContainer());
            }
        });
    }

    public static DbSchema getSchema()
    {
        return DbSchema.get(SCHEMA_NAME, DbSchemaType.Module);
    }

    public SqlDialect getSqlDialect()
    {
        return getSchema().getSqlDialect();
    }

    @Override
    public TableInfo createTable(String name, ContainerFilter cf)
    {
        if (TABLE_EXPERIMENT_ANNOTATIONS.equalsIgnoreCase(name))
        {
            return new ExperimentAnnotationsTableInfo(this, cf);
        }
        if (TABLE_JOURNAL_EXPERIMENT.equalsIgnoreCase(name))
        {
            return new JournalExperimentTableInfo(this, cf);
        }
        if (TABLE_SUBMISSION.equalsIgnoreCase(name))
        {
            return new SubmissionTableInfo(this, cf);
        }

        if (TABLE_JOURNAL.equalsIgnoreCase(name))
        {
            FilteredTable<PanoramaPublicSchema> result = new FilteredTable<>(getSchema().getTable(name), this, cf);
            result.wrapAllColumns(true);
            var projectCol = result.getMutableColumn(FieldKey.fromParts("Project"));
            projectCol.setFk(new ContainerForeignKey(result.getUserSchema()));
            var supportContainerCol = result.getMutableColumn(FieldKey.fromParts("SupportContainer"));
            supportContainerCol.setFk(new ContainerForeignKey(result.getUserSchema()));
            return result;
        }

        if(TABLE_PX_XML.equalsIgnoreCase(name))
        {
            return getFilteredPxXmlTable(name, cf);
        }

        if (TABLE_SPEC_LIB_INFO.equalsIgnoreCase(name))
        {
            return new SpecLibInfoTableInfo(this, cf);
        }

        if (TABLE_LIB_DEPENDENCY_TYPE.equalsIgnoreCase(name))
        {
            EnumTableInfo<SpecLibDependencyType> tableInfo = new EnumTableInfo<>(
                    SpecLibDependencyType.class,
                    this,
                    SpecLibDependencyType::getLabel,
                    true,
                    "Types of dependencies on a spectral library");

            var viewColumn = tableInfo.getMutableColumn("Value");
            viewColumn.setLabel("Dependency Type");
            return tableInfo;
        }

        if (TABLE_LIB_SOURCE_TYPE.equalsIgnoreCase(name))
        {
            EnumTableInfo<SpecLibSourceType> tableInfo = new EnumTableInfo<>(
                    SpecLibSourceType.class,
                    this,
                    SpecLibSourceType::getLabel,
                    true,
                    "Spectral library source types");

            var viewColumn = tableInfo.getMutableColumn("Value");
            viewColumn.setLabel("Library Source");
            return tableInfo;
        }

        if (TABLE_DATA_VALIDATION.equalsIgnoreCase(name))
        {
            return new DataValidationTableInfo(this, cf);
        }

        if (TABLE_SKYLINE_DOC_VALIDATION.equalsIgnoreCase(name))
        {
            var table = new PanoramaPublicTable(PanoramaPublicManager.getTableInfoSkylineDocValidation(), this, cf, ValidationToExpAnnotJoins);
            var sampleFileCountsCol = DataValidationTableInfo.createCountsColumn(table,
                    PanoramaPublicManager.getTableInfoSkylineDocSampleFile(), "SkylineDocValidationId",
                    PanoramaPublicSchema.TABLE_SKYLINE_DOC_SAMPLE_FILE, "SampleFiles", table.getContainerContext());
            table.addColumn(sampleFileCountsCol);
            var displayCols = new ArrayList<>(table.getDefaultVisibleColumns());
            displayCols.add(FieldKey.fromParts(sampleFileCountsCol.getName()));
            table.setDefaultVisibleColumns(displayCols);
            return table;
        }

        if (TABLE_SKYLINE_DOC_SAMPLE_FILE.equalsIgnoreCase(name))
        {
            return new PanoramaPublicTable(PanoramaPublicManager.getTableInfoSkylineDocSampleFile(), this, cf,
                    List.of(new InnerJoinClause(null, "SkylineDocValidationId", PanoramaPublicManager.getTableInfoSkylineDocValidation(), "doc", "id"),
                            new InnerJoinClause("doc", "ValidationId", PanoramaPublicManager.getTableInfoDataValidation(), "v", "id"),
                            new InnerJoinClause("v", "ExperimentAnnotationsId", PanoramaPublicManager.getTableInfoExperimentAnnotations(), "exp", "id")));
        }

        if (TABLE_SPEC_LIB_VALIDATION.equalsIgnoreCase(name))
        {
            var table = new PanoramaPublicTable(PanoramaPublicManager.getTableInfoSpecLibValidation(), this, cf, ValidationToExpAnnotJoins);
            var sourceFileCountCol = DataValidationTableInfo.createCountsColumn(table,
                    PanoramaPublicManager.getTableInfoSpecLibSourceFile(), "SpecLibValidationId",
                    PanoramaPublicSchema.TABLE_SPEC_LIB_SOURCE_FILE, "SourceFiles", table.getContainerContext());
            table.addColumn(sourceFileCountCol);
            var displayCols = new ArrayList<>(table.getDefaultVisibleColumns());
            displayCols.add(FieldKey.fromParts(sourceFileCountCol.getName()));
            table.setDefaultVisibleColumns(displayCols);
            return table;
        }

        if (TABLE_SPEC_LIB_SOURCE_FILE.equalsIgnoreCase(name))
        {
            var table = new PanoramaPublicTable(PanoramaPublicManager.getTableInfoSpecLibSourceFile(), this, cf,
                    List.of(new InnerJoinClause(null, "SpecLibValidationId", PanoramaPublicManager.getTableInfoSpecLibValidation(), "l", "id"),
                            new InnerJoinClause("l", "ValidationId", PanoramaPublicManager.getTableInfoDataValidation(), "v", "id"),
                            new InnerJoinClause("v", "ExperimentAnnotationsId", PanoramaPublicManager.getTableInfoExperimentAnnotations(), "exp", "id")));
            var sourceTypeCol = table.getMutableColumn("SourceType");
            if (sourceTypeCol != null)
            {
                sourceTypeCol.setFk(QueryForeignKey.from(this, cf).to(PanoramaPublicSchema.TABLE_LIBRARY_SOURCE_FILE_TYPE, "RowId", null));
            }
            return table;
        }

        if (TABLE_MODIFICATION_VALIDATION.equalsIgnoreCase(name))
        {
            var table = new PanoramaPublicTable(PanoramaPublicManager.getTableInfoModificationValidation(), this, cf, ValidationToExpAnnotJoins);
            var modTypeCol = table.getMutableColumn("ModType");
            if (modTypeCol != null)
            {
                modTypeCol.setFk(QueryForeignKey.from(this, cf).to(PanoramaPublicSchema.TABLE_MOD_TYPE, "RowId", null));
            }
            return table;
        }

        if (TABLE_PX_STATUS.equalsIgnoreCase(name))
        {
            EnumTableInfo<PxStatus> tableInfo = new EnumTableInfo<>(
                    PxStatus.class,
                    this,
                    PxStatus::getLabel,
                    true,
                    "Status after validating a dataset for ProteomeXchange");

            tableInfo.getMutableColumn("Value").setLabel("PX Status");
            return tableInfo;
        }

        if (TABLE_LIBRARY_SOURCE_FILE_TYPE.equalsIgnoreCase(name))
        {
            return new EnumTableInfo<>(
                    LibrarySourceFileType.class,
                    this,
                    LibrarySourceFileType::name,
                    true,
                    "Spectral library source file type");
        }

        if (TABLE_MOD_TYPE.equalsIgnoreCase(name))
        {
            return new EnumTableInfo<>(
                    ModType.class,
                    this,
                    ModType::name,
                    true,
                    "Modification type (structural or isotopic)");
        }

        return null;
    }

    @NotNull
    private TableInfo getFilteredPxXmlTable(String name, ContainerFilter cf)
    {
        FilteredTable<PanoramaPublicSchema> result = new FilteredTable<>(getSchema().getTable(name), this, cf)
        {
            @Override
            protected void applyContainerFilter(ContainerFilter filter)
            {
                // Don't apply the container filter normally, let us apply it in our wrapper around the normally generated SQL
            }

            @Override
            public SQLFragment getFromSQL(String alias)
            {
                // This table does not have a Container column so we will join it to the JournalExperiment and ExperimentAnnotations
                // tables to filter by the Container of the copied experiment.
                SQLFragment sql = new SQLFragment("(SELECT X.* FROM ");
                sql.append(super.getFromSQL("X"));
                sql.append(" ");

                if (getContainerFilter() != ContainerFilter.EVERYTHING)
                {
                    SQLFragment joinToExpAnnotSql = new SQLFragment("INNER JOIN ");
                    joinToExpAnnotSql.append(PanoramaPublicManager.getTableInfoSubmission(), "s");
                    joinToExpAnnotSql.append(" ON (s.journalExperimentId = X.JournalExperimentId) ");
                    joinToExpAnnotSql.append(" INNER JOIN ");
                    joinToExpAnnotSql.append(PanoramaPublicManager.getTableInfoExperimentAnnotations(), "exp");
                    joinToExpAnnotSql.append(" ON (exp.id = s.CopiedExperimentId) ");

                    sql.append(joinToExpAnnotSql);

                    sql.append(" WHERE ");
                    sql.append(getContainerFilter().getSQLFragment(getSchema(), new SQLFragment("exp.Container")));
                }
                sql.append(") ");
                sql.append(alias);

                return sql;
            }
        };
        result.wrapAllColumns(true);
        return result;
    }

    @Override
    public @NotNull QueryView createView(ViewContext context, @NotNull QuerySettings settings, @Nullable BindException errors)
    {
        QueryView view;
        if (TABLE_SPEC_LIB_INFO.equalsIgnoreCase(settings.getQueryName())
        || TABLE_DATA_VALIDATION.equalsIgnoreCase(settings.getQueryName()))
        {
            // Show the delete icon in the toolbar but not the insert or update icons
            view = new QueryView(this, settings, errors)
            {
                @Override
                protected boolean canDelete()
                {
                    return true;
                }

                @Override
                protected boolean canInsert()
                {
                    return false;
                }

                @Override
                public boolean showImportDataButton()
                {
                    return false;
                }

                @Override
                protected boolean canUpdate()
                {
                    return false;
                }
            };
        }
        else
        {
            view = super.createView(context, settings, errors);
        }

        if (TABLE_SKYLINE_DOC_VALIDATION.equalsIgnoreCase(settings.getQueryName())
                || TABLE_SKYLINE_DOC_SAMPLE_FILE.equalsIgnoreCase(settings.getQueryName())
                || TABLE_MODIFICATION_VALIDATION.equalsIgnoreCase(settings.getQueryName())
                || TABLE_SPEC_LIB_VALIDATION.equalsIgnoreCase(settings.getQueryName())
                || TABLE_SPEC_LIB_SOURCE_FILE.equalsIgnoreCase(settings.getQueryName()))
        {
            view.disableContainerFilterSelection(); // No need for a container filter
        }
        return view;
    }

    @Override
    public Set<String> getTableNames()
    {
        CaseInsensitiveHashSet hs = new CaseInsensitiveHashSet();
        hs.add(TABLE_JOURNAL);
        hs.add(TABLE_JOURNAL_EXPERIMENT);
        hs.add(TABLE_SUBMISSION);
        hs.add(TABLE_EXPERIMENT_ANNOTATIONS);
        hs.add(TABLE_PX_XML);
        hs.add(TABLE_SPEC_LIB_INFO);
        hs.add(TABLE_DATA_VALIDATION);
        return hs;
    }

    private static final List<InnerJoinClause> ValidationToExpAnnotJoins = List.of(
            new InnerJoinClause(null, "ValidationId", PanoramaPublicManager.getTableInfoDataValidation(), "v", "id"),
            new InnerJoinClause("v", "ExperimentAnnotationsId", PanoramaPublicManager.getTableInfoExperimentAnnotations(), "exp", "id"));
    
    public static class InnerJoinClause
    {
        private final String _tableAlias;
        private final String _joinCol;
        private final TableInfo _joinTable;
        private final String _joinTableAlias;
        private final String _joinTableCol;

        public InnerJoinClause(@Nullable String tableAlias, @NotNull String joinCol, @NotNull TableInfo joinTable, @NotNull String joinTableAlias, @NotNull String joinTableCol)
        {
            _tableAlias = tableAlias;
            _joinCol = joinCol;
            _joinTable = joinTable;
            _joinTableAlias = joinTableAlias;
            _joinTableCol = joinTableCol;
        }

        public SQLFragment toSql()
        {
            return new SQLFragment(" INNER JOIN ")
                    .append(_joinTable, _joinTableAlias)
                    .append(" ON ")
                    .append(_joinTableAlias).append(".").append(_joinTableCol)
                    .append(" = ")
                    .append(_tableAlias != null ? _tableAlias + "." : "").append(_joinCol)
                    .append(" ");
        }

        public String getJoinTableAlias()
        {
            return _joinTableAlias;
        }

        public String getJoinCol()
        {
            return _joinCol;
        }
    }
}
