package com.rafptor.parser.model;

import com.rafptor.parser.modca.BeginActiveEnvironmentGroup;
import com.rafptor.parser.modca.BeginDocument;
import com.rafptor.parser.modca.BeginObjectEnvironmentGroup;
import com.rafptor.parser.modca.BeginPage;
import com.rafptor.parser.modca.BeginResourceGroup;
import com.rafptor.parser.modca.EndActiveEnvironmentGroup;
import com.rafptor.parser.modca.EndDocument;
import com.rafptor.parser.modca.EndObjectEnvironmentGroup;
import com.rafptor.parser.modca.EndPage;
import com.rafptor.parser.modca.EndResourceGroup;
import com.rafptor.parser.modca.IncludeObject;
import com.rafptor.parser.modca.IncludePageOverlay;
import com.rafptor.parser.modca.IncludePageSegment;
import com.rafptor.parser.modca.MapCodedFont;
import com.rafptor.parser.modca.MapDataResource;
import com.rafptor.parser.modca.NoOperation;
import com.rafptor.parser.modca.PresentationTextData;
import com.rafptor.parser.modca.TagLogicalElement;
import com.rafptor.parser.modca.UnknownStructuredField;

/**
 * Sealed interface implemented by every typed Structured Field.
 */
public sealed interface AfpStructuredField permits
        BeginDocument,
        EndDocument,
        BeginPage,
        EndPage,
        BeginActiveEnvironmentGroup,
        EndActiveEnvironmentGroup,
        BeginResourceGroup,
        EndResourceGroup,
        BeginObjectEnvironmentGroup,
        EndObjectEnvironmentGroup,
        IncludeObject,
        IncludePageOverlay,
        IncludePageSegment,
        MapCodedFont,
        MapDataResource,
        TagLogicalElement,
        NoOperation,
        PresentationTextData,
        UnknownStructuredField {

    StructuredFieldId id();
}
