# scRNAseqTanglesClustering

## Overview
This repository contains a Java-based framework and graphical user interface (GUI) developed as part of a master’s thesis.  
The framework implements the Tangles clustering pipeline described in the thesis and provides tools for experimentation, visualization, and benchmarking with different parameter configurations.

The GUI is intended as a testing and exploration environment rather than a production-ready application.

## Features
- Execution of the full Tangles clustering pipeline with configurable parameters
- Visualization of clustering results, cuts, and tangle search trees
- Support for both hard and soft clusterings
- Interactive exploration of tangle search trees
- Benchmarking and comparison of multiple configurations
- Optional integration with the Scanpy pipeline implemented in Python
- Saving and loading of configuration files for reproducibility

## Installation
- The main application is launched by running  
  `src/main/java/main/Main.java`.
- Running the application requires **Java** and **Maven**.
- The program was tested using **Java 21** and **IntelliJ IDEA**.
- A Python script is included to support certain features (e.g., integration with Scanpy).  
  Installing Python is required only if these features are used.

## Python Dependencies (Optional)
To enable all features related to the Python-based Scanpy pipeline, the following Python libraries are required:

- `scanpy`
- `anndata`
- `pandas`
- `numpy`
- `scikit-learn`
- `kneed`

These dependencies are only required when using the Python integration and are not needed to run the main Java application.

## Usage
- The application provides a graphical user interface with multiple views for clustering, visualization, and testing.
- Users can load a single data set and experiment with different configurations of the Tangles clustering pipeline.
- It is also possible to load a folder containing multiple data sets and run benchmark tests to compare several pipeline configurations.

## Supported Data Formats
The program supports loading data sets from the following formats:
- `.csv`
- `.h5ad` (AnnData format commonly used in Scanpy)

## Configuration Files
All parameter settings for the Tangles pipeline can be saved to and loaded from configuration files.  
This enables reproducible experiments and systematic comparison of configurations.

## Relation to the Thesis
This framework and GUI serve as supporting tools for the master’s thesis.  
They are primarily intended for demonstration, experimentation, and evaluation of the methods presented in the thesis.
