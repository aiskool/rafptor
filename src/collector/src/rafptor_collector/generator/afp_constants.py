"""AFP / MO:DCA structured-field identifiers used by the simulator.

Source: AFP Consortium MO:DCA Reference, chapter 5.
Ids are the same three-byte sequences the parser (Module 1) dispatches on.
"""

from __future__ import annotations

AFP_CC = 0x5A

# Document level
SF_BDT = bytes([0xD3, 0xA8, 0xA8])   # Begin Document
SF_EDT = bytes([0xD3, 0xA9, 0xA8])   # End Document

# Page level
SF_BPG = bytes([0xD3, 0xA8, 0xAF])   # Begin Page
SF_EPG = bytes([0xD3, 0xA9, 0xAF])   # End Page

# Active Environment Group
SF_BAG = bytes([0xD3, 0xA8, 0xC9])   # Begin Active Env Group
SF_EAG = bytes([0xD3, 0xA9, 0xC9])   # End Active Env Group

# Resource Group
SF_BRG = bytes([0xD3, 0xA8, 0xC6])   # Begin Resource Group
SF_ERG = bytes([0xD3, 0xA9, 0xC6])   # End Resource Group

# Content
SF_PTX = bytes([0xD3, 0xEE, 0x9B])   # Presentation Text Data
SF_MCF = bytes([0xD3, 0xAB, 0x8A])   # Map Coded Font
SF_IPO = bytes([0xD3, 0xAF, 0xD8])   # Include Page Overlay
SF_IPS = bytes([0xD3, 0xAF, 0x5F])   # Include Page Segment

# Metadata
SF_TLE = bytes([0xD3, 0xA0, 0x90])   # Tag Logical Element
SF_NOP = bytes([0xD3, 0xEE, 0xEE])   # No Operation

# FOCA (character set)
SF_FND = bytes([0xD3, 0xA6, 0x89])   # Font Descriptor
SF_FNC = bytes([0xD3, 0xA7, 0x89])   # Font Control
SF_FNI = bytes([0xD3, 0x8C, 0x89])   # Font Index
SF_FNP = bytes([0xD3, 0x8E, 0x89])   # Font Patterns

# PTOCA control-sequence function types
PTOCA_SCFL = 0xF1   # Set Coded Font Local
PTOCA_AMB = 0xD2    # Absolute Move Baseline
PTOCA_AMI = 0xC6    # Absolute Move Inline
PTOCA_RMI = 0xC8    # Relative Move Inline
PTOCA_TRN = 0xDA    # Transparent Data
PTOCA_DIR = 0xE6    # Draw I-axis Rule
PTOCA_DBR = 0xE4    # Draw B-axis Rule

# Triplet identifiers
TRIPLET_FULLY_QUALIFIED_NAME = 0x02
TRIPLET_ATTRIBUTE_VALUE = 0x36
